package com.handy.kafka

import collection.JavaConverters._
import java.util.Properties
import scalaz.concurrent.Task
import org.apache.kafka.clients.consumer.{KafkaConsumer, ConsumerRecord}


object EventConsumer {
  type Consumer = KafkaConsumer[String, Array[Byte]]
  type KafkaRecord = ConsumerRecord[String, Array[Byte]]
  

  def subscribe(topics: String*) = Task {
    val kafkaProps = getClass.getResourceAsStream("/kafka.properties")
    val props = new Properties()
    props.load(kafkaProps)
    val consumer: Consumer = new KafkaConsumer(props)
    consumer.subscribe(topics.asJava)
    consumer
  }
}
