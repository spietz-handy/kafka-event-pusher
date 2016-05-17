package com.handy.kafka

import scala.concurrent.duration._

import collection.JavaConverters._

import scalaz._
import Scalaz._
import scalaz.syntax._
import scalaz.concurrent.Task
import scalaz.stream._
import scalaz.stream.time.every

import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.blaze.SimpleHttp1Client

import scodec.bits.ByteVector

import com.handy.kafka.HttpSender._
import com.handy.kafka.EventConsumer._
 

case class Resources(consumer: Consumer, client: Client, url: Uri)

object EventPusher {

  val syncDuration = 1 seconds

  def pusher(r: Resources): Process[Task, Unit] =
    Process.repeatEval (
      Task(r.consumer.poll(1000).asScala.toSeq)
    ) flatMap (recs => 
      Process(recs: _*).toSource
    ) flatMap (rec =>
      Process eval sendRecord(rec, r)
    ) zip (
      every(syncDuration)
    ) flatMap { case (rec, sync) => 
      Process eval_ (
        if (sync) commitOffset(r.consumer, rec)
        else Task.now(())
      )
    } onComplete (Process eval_ Task(r.consumer.close()))

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
    val stream = Process.await(resources)(pusher)

    println(stream.run.attemptRun)
  }
}
