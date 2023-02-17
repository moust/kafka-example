kafka-example
=============

This project is a simple example of a Kafka producer producing order records, which are processed by a Kafka stream to produce order validations comsumed by a Kafka consumer.

### Run stack

```shell
docker-compose up -d
```

### Create topics

````shell
kafka-topics.sh --bootstrap-server localhost:9092 --create --topic orders

kafka-topics.sh --bootstrap-server localhost:9092 --create --topic order-validations
````

### Run order producer

````shell
sbt "runMain com.github.moust.OrderProducer"
````

### Run order validation stream

````shell
sbt "runMain com.github.moust.OrderValidationConsumer"
````

### Read order-validations topic (debug)

```shell
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic order-validations --from-beginning
```

### Run order consumer stream

````shell
sbt "runMain com.github.moust.OrderProducer"
````