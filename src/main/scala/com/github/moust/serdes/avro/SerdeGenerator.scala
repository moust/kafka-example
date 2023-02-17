package com.github.moust.serdes.avro

import com.sksamuel.avro4s._
import com.sksamuel.avro4s.kafka.GenericSerde
import io.confluent.kafka.schemaregistry.client.{
  CachedSchemaRegistryClient,
  MockSchemaRegistryClient,
  SchemaRegistryClient,
}
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.scala.serialization.Serdes

trait SerdeGenerator {
  def serdeFor[A >: Null: Encoder: Decoder: SchemaFor]: Serde[A]
}

object SerdeGenerator {

  def fromUrl(schemaRegistryUrl: String): SerdeGenerator =
    if (schemaRegistryUrl.toLowerCase() == "none") {
      new BasicSerdeGenerator(JsonFormat)
    } else {
      lazy val serdeConfig: Map[String, _] = Map(
        AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> schemaRegistryUrl
      )

      def schemaRegistry: SchemaRegistryClient =
        if (schemaRegistryUrl.startsWith("mock://"))
          new MockSchemaRegistryClient()
        else
          new CachedSchemaRegistryClient(schemaRegistryUrl, 1000)

      new SchemaRegistryBasedSerdeGenerator(schemaRegistry, serdeConfig)
    }

}

class BasicSerdeGenerator(format: AvroFormat) extends SerdeGenerator {

  override def serdeFor[A >: Null: Encoder: Decoder: SchemaFor]: Serde[A] =
    new GenericSerde[A](format)
}

/** Generate serde based on Confluent schema registry.
  *
  * @param schemaRegistry schema registry client
  * @param serdeConfig configuration for the serde
  * @param isSerdeForRecordKeys whether is for key or value
  */
class SchemaRegistryBasedSerdeGenerator(
  schemaRegistry: => SchemaRegistryClient,
  serdeConfig: Map[String, _],
  isSerdeForRecordKeys: Boolean = false)
    extends SerdeGenerator {
  import scala.jdk.CollectionConverters._

  def serde: GenericAvroSerde = {
    val s = new GenericAvroSerde(schemaRegistry)
    s.configure(serdeConfig.asJava, isSerdeForRecordKeys)

    s
  }

  override def serdeFor[A >: Null: Encoder: Decoder: SchemaFor]: Serde[A] = {
    val rfA: RecordFormat[A] = RecordFormat[A]

    Serdes.fromFn(
      (topic, data) => serde.serializer().serialize(topic, rfA.to(data)),
      (topic, bytes) => Option(rfA.from(serde.deserializer().deserialize(topic, bytes))),
    )
  }
}
