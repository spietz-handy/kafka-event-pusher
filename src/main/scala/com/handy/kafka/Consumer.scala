package com.handy.kafka

import collection.JavaConverters._
import java.util.Properties
import scalaz.concurrent.Task
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.clients.consumer._

import org.log4s._


object Consumer {
  val logger = getLogger

  type Consumer = KafkaConsumer[String, Array[Byte]]
  type KafkaRecord = ConsumerRecord[String, Array[Byte]]
  
  def subscribe(topic: String, groupId: String) = Task.delay {
    val kafkaProps = getClass.getResourceAsStream("/kafka.properties")
    val props = new Properties()
    props.load(kafkaProps)
    props.setProperty("group.id", groupId)
    val consumer: Consumer = new KafkaConsumer(props)
    logger.info(s"subscribing to topic $topic")
    consumer.subscribe(List(topic).asJava)
    consumer
  }
 
  def commitOffset(consumer: Consumer)(record: KafkaRecord) = Task.delay {
    val topicPart = new TopicPartition(record.topic, record.partition)
    val offset = new OffsetAndMetadata(record.offset)
    consumer.commitSync(Map(topicPart -> offset).asJava)
    logger.info(
      s"""
      |committed offset: ${record.offset}
      |on topic: ${record.topic}
      |and partition: ${record.partition}
      """.stripMargin
    )
  }
}
