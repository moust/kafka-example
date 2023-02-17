package com.github.moust

import java.util.{Properties, Random, UUID}

import scala.util.Try

import com.github.moust.domain._
import com.github.moust.serdes.avro.SerdeGenerator
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.streams.scala.serialization.Serdes
import org.slf4j.{Logger, LoggerFactory}

object OrderProducer extends App {
  implicit val logger: Logger = LoggerFactory.getLogger(getClass)

  private val props = new Properties
  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
  props.put(ProducerConfig.ACKS_CONFIG, "1")

  // private val orderSerde = JsonSerde[Order]
  private val serdeGenerator = SerdeGenerator.fromUrl("http://localhost:8081/")
  private val orderSerde     = serdeGenerator.serdeFor[Order]

  private val producer = new KafkaProducer[String, Order](
    props,
    Serdes.stringSerde.serializer(),
    orderSerde.serializer(),
  )

  for (_ <- 1 to 10) {
    val orderId    = UUID.randomUUID().toString
    val consumerId = UUID.randomUUID().toString
    val random     = new Random()
    val order = Order(
      orderId,
      consumerId,
      OrderState.Created,
      Product.values(random.nextInt(Product.values.length)),
      random.nextInt(3) + 1,
      random.nextInt(1000).toDouble,
    )
    val record = new ProducerRecord("orders", order.id, order)
    producer.send(record)
    logger.debug(s"produced order: $order")
  }

  Try {
    producer.flush()
    producer.close()
  }.recover {
    case error: InterruptedException => logger.error("failed to flush and close the producer", error)
    case error                       => logger.error("An unexpected error occurs while producing the show collection", error)
  }
}
