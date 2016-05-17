package com.handy.kafka

import collection.JavaConverters._
import java.util.Properties
import scalaz.concurrent.Task
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.clients.consumer._


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
  
  def commitOffset(consumer: Consumer, record: KafkaRecord) = Task {
    val tp = new TopicPartition(record.topic, record.partition)
    val oam = new OffsetAndMetadata(record.offset)
    consumer.commitSync(Map(tp -> oam).asJava)
    println(
      s"""
      |committed offset: ${record.offset}
      |on topic: ${record.topic}
      |and partition: ${record.partition}
      """.stripMargin
    )
  }

}
