package com.handy.kafka

import scala.io.Source

import java.util.Properties

import collection.JavaConverters._

import scalaz.stream._
import scalaz._
import Scalaz._
import scalaz.concurrent.Task

import org.http4s._
import org.http4s.client.Client
import org.http4s.client.blaze.SimpleHttp1Client
import org.http4s.dsl._
import org.http4s.Header

import scodec.bits.ByteVector

import org.apache.kafka.clients.consumer.{KafkaConsumer, ConsumerRecord}


case class KafkaSendError(offset: Int) extends Throwable {
  override def getMessage = "Failed to send message"
}

object EventPusher {
  type Consumer = KafkaConsumer[String, Array[Byte]]
  
  case class Resources(consumer: Consumer, client: Client, url: Uri)

  def subscribe(topics: String*) = Task {
    val res = getClass.getResourceAsStream("/kafka.properties")
    val props = new Properties()
    props.load(res)
    val consumer: KafkaConsumer[String, Array[Byte]] = new KafkaConsumer(props)
    consumer.subscribe(topics.asJava)
    consumer
  }


  def sender(client: Client, url: Uri, offset: Long, bv: ByteVector) = {
    println("making request")
    val headers = Headers(Header("Content-Type", "avro/binary"))
    val req = Request(POST, url, body = Process(bv).toSource, headers = headers)
    client.fetch(req)(handle)
  }

  def handle(r: Response) = Task {
    val s = r.status
    if (s.isSuccess) {
      println(s"request completed ${s.toString}")
    } else {
      throw new Exception(s"${s.toString}")
    }
  }

  def listener(r: Resources): Process[Task, Unit] =
    Process.repeatEval (
      Task(r.consumer.poll(1000).asScala.toSeq)
    ) flatMap (recs => 
      Process(recs: _*).toSource
    ) flatMap (rec =>
      Process eval_ sender(r.client, r.url, rec.offset, ByteVector(rec.value))
    ) onComplete (Process eval_ Task(r.consumer.close()))
 

  def main(args: Array[String]): Unit = {
    val consumer = subscribe("credits_created_schema_1")
    val resources = consumer.map(c =>
        Resources(c, SimpleHttp1Client(), uri("http://10.10.60.116/credits_created"))
    )
    val res = Process.await(resources)(listener).run.run
  }
}
