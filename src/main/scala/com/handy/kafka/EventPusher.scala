package com.handy.kafka

import scala.concurrent.duration._
import scala.annotation.tailrec

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
  val client = SimpleHttp1Client()
  val syncDuration = 1 seconds
  val retryDurs = Vector(
    200 millis , 
    500 millis ,
    1   seconds,
    5   seconds,
    20  seconds,
    1   minute
  )

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

    val uri = uriFromString(uriString).valueOr(throw _)
    val resources = subscribe(topic).map(consumer =>
      Resources(consumer, client, uri)
    )
    
    val stream = Process.await(resources)(pusher)
    val runStream = Task(
      stream.run.attemptRun.fold(e => e.getMessage, _ => "")
    )

    val durations = (Process(retryDurs: _*) ++ Process.constant(retryDurs.last))

    val retryTimes = durations.flatMap ( d =>
        Process eval Task {
          Thread.sleep(d.toMillis)
          s"retrying in $d ..."
        }
      )

    Process.repeatEval(runStream)
           .interleave(retryTimes)
           .flatMap(msg => Process eval_ Task(println(msg)))
           .run.run
  }
}
