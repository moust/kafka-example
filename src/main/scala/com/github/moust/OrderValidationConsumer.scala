package com.github.moust

import com.github.moust.domain._
import com.github.moust.serdes.JsonSerde

import io.circe.generic.auto._

import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.streams.scala.serialization.Serdes

import org.slf4j.{Logger, LoggerFactory}

import java.time.Duration
import java.util.Properties
import java.util.concurrent.{Executors, TimeUnit}

import scala.jdk.CollectionConverters._
import scala.util.Try

object OrderValidationConsumer extends App {
  implicit val logger: Logger = LoggerFactory.getLogger(getClass)

  private val props = new Properties
  props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
  props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-validations")
  // only in dev mode, start pulling from beginning
  props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  private val scheduler = Executors.newSingleThreadScheduledExecutor()
  scheduler.schedule(() => {
    val orderValidationSerde = JsonSerde[OrderValidation]

    logger.debug("Creating kafka consumer")
    val consumer = new KafkaConsumer[String, OrderValidation](
      props,
      Serdes.stringSerde.deserializer(),
      orderValidationSerde.deserializer()
    )

    consumer.subscribe(("order-validations" :: Nil).asJava)

    while (!scheduler.isShutdown) {
      Try {
        logger.debug("polling the new records...")
        val records = consumer.poll(Duration.ofSeconds(5))
        records.iterator().asScala.toVector
      }.recover { case error =>
        logger.error("☠️ ☠️ ☠️", error)
        Vector.empty
      }.foreach { records =>
        records.foreach { record =>
          logger.info(s"${record.key()} -> ${record.value()}")
        }
      }
    }

    logger.info("Closing the kafka consumer")
    Try(consumer.close()).recover {
      case error => logger.error("Failed to close the kafka consumer", error)
    }

  }, 1, TimeUnit.SECONDS)

  sys.addShutdownHook {
    scheduler.shutdown()
    scheduler.awaitTermination(10, TimeUnit.SECONDS)
    ()
  }
}
