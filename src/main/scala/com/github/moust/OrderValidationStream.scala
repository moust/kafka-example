package com.github.moust

import java.time.Duration
import java.util.Properties

import com.github.moust.domain._
import com.github.moust.serdes.avro.SerdeGenerator
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig, Topology}
import org.apache.kafka.streams.kstream.{Named, SessionWindows, Windowed}
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream._
import org.apache.kafka.streams.scala.serialization.Serdes
import org.slf4j.{Logger, LoggerFactory}

object OrderValidationStream extends App {
  implicit val logger: Logger = LoggerFactory.getLogger(getClass)

  private val props = new Properties
  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "orders-validation")
  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
  props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.stringSerde.getClass)
  props.put(StreamsConfig.STATE_DIR_CONFIG, "./docker/data/kafka")

  private val FRAUD_LIMIT = 1000

  private val builder = new StreamsBuilder

  // private val orderSerde = JsonSerde[Order]
  // private val orderValueSerde = JsonSerde[OrderValue]
  // private val orderValidationSerde = JsonSerde[OrderValidation]
  private val serdeGenerator       = SerdeGenerator.fromUrl("http://localhost:8081/")
  private val orderSerde           = serdeGenerator.serdeFor[Order]
  private val orderValueSerde      = serdeGenerator.serdeFor[OrderValue]
  private val orderValidationSerde = serdeGenerator.serdeFor[OrderValidation]

  private val orders: KStream[String, Order] = builder
    .stream[String, Order]("orders")(Consumed.`with`(Serdes.stringSerde, orderSerde))
    .filter((_, order) => order.state == OrderState.Created)

  // Create an aggregate of the total value by customer and hold it with the order. We use session windows to detect periods of activity.
  private val aggregate: KTable[Windowed[String], OrderValue] = orders
    .groupBy((_, order) => order.customerId)(Grouped.`with`(Serdes.stringSerde, orderSerde))
    .windowedBy(SessionWindows.ofInactivityGapWithNoGrace(Duration.ofHours(1)))
    .aggregate(
      OrderValue(Order("", "", OrderState.Created, Product.Jumpers, 0, 0.0), 0.0)
    )(
      //Calculate running total for each customer within this window
      (_, order, total) => OrderValue(order, total.value + order.quantity * order.price),
      (_, a, b) => OrderValue(b.order, a.value + b.value),
    )(Materialized.`with`(Serdes.stringSerde, orderValueSerde))

  //Ditch the windowing and rekey//Ditch the windowing and rekey

  private val ordersWithTotals: KStream[String, OrderValue] = aggregate
    .toStream((windowedKey, _) => windowedKey.key)
    .filter((_, v) =>
      v != null
    ) //When elements are evicted from a session window they create delete events. Filter these out.
    .selectKey((_, orderValue) => orderValue.order.id)

  //Now branch the stream into two, for pass and fail, based on whether the windowed total is over Fraud Limit
  private val forks: Map[String, KStream[String, OrderValue]] = ordersWithTotals
    .split(Named.as("limit-"))
    .branch((_, orderValue) => orderValue.value >= FRAUD_LIMIT, Branched.as("above"))
    .branch((_, orderValue) => orderValue.value < FRAUD_LIMIT, Branched.as("below"))
    .noDefaultBranch()

  forks.foreach {
    case ("limit-above", orderValues) =>
      orderValues
        .mapValues(orderValue =>
          OrderValidation(orderValue.order.id, OrderValidationType.FraudCheck, OrderValidationResult.Fail)
        )
        .to("order-validations")(Produced.`with`(Serdes.stringSerde, orderValidationSerde))
    case ("limit-below", orderValues) =>
      orderValues
        .mapValues(orderValue =>
          OrderValidation(orderValue.order.id, OrderValidationType.FraudCheck, OrderValidationResult.Pass)
        )
        .to("order-validations")(Produced.`with`(Serdes.stringSerde, orderValidationSerde))
    case _ => ()
  }

  private val topology: Topology = builder.build()

  // logger.debug(topology.describe().toString)

  private val stream = new KafkaStreams(topology, props)
  stream.start()
}
