package com.handy.kafka

import scala.concurrent.duration._

import collection.JavaConverters._

import scalaz._
import Scalaz._
import scalaz.syntax._
import scalaz.concurrent.Task
import scalaz.stream._

import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.blaze.SimpleHttp1Client

import scodec.bits.ByteVector

import com.handy.kafka.HttpSender._
import com.handy.kafka.EventConsumer._

  
case class Resources(consumer: Consumer, client: Client, url: Uri)

object EventPusher {
  val retryDurs = List(200 millis, 1 seconds, 5 seconds)

  def pusher(r: Resources): Process[Task, Unit] =
    Process.repeatEval (
      Task(r.consumer.poll(1000).asScala.toSeq)
    ) flatMap (recs => 
      Process(recs: _*).toSource
    ) flatMap (rec =>
      Process eval_ sendRecord(rec, r, retryDurs)
    ) onComplete (Process eval_ Task(r.consumer.close()))
 
  def handleTask(res: Throwable \/ Unit): Unit = res match {
    case \/-(()) => println("yoooooo")
    case -\/(e: Throwable) => throw e
  }

  def main(args: Array[String]): Unit = {
    val Array(topic, uriString) = args 
    val client = SimpleHttp1Client()

    def resourcesFor(topic: String, uriString: String) = {
      val uri = uriFromString(uriString) match {
        case \/-(uri: Uri) => uri
        case -\/(e: UriParseError) => throw e
      }

      subscribe(topic).map(consumer =>
        Resources(consumer, client, uri)
      )
    }

    val resources = resourcesFor(topic, uriString)
    val resources2 = resourcesFor("user_schema_1", "http://www.google.com")

    val s1 = Process.await(resources)(pusher)
    val s2 = Process.await(resources2)(pusher)

    val res = s1 merge s2

    println(res.run.attemptRun)
  }
}
