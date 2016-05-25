package com.handy.kafka

import scala.concurrent.duration._
import scala.annotation.tailrec

import collection.JavaConverters._

import org.log4s._

import scalaz._
import Scalaz._
import scalaz.syntax._
import scalaz.concurrent.Task
import scalaz.stream._
import scalaz.stream.Process.constant
import scalaz.stream.time.every

import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.blaze._

import scodec.bits.ByteVector

import com.handy.kafka.syntax._
import com.handy.kafka.util._
import com.handy.kafka.Initializer._ 
import com.handy.kafka.Endpoint._
import com.handy.kafka.Consumer._


object EventPusher {
  val logger = getLogger

  /** 
   * This is the main stream responsible for consuming kafka events
   * and pushing them to downstream service. One of these is created
   * for every topic/uri endpoint pair.
   */
  def pusher(r: Resources): Process[Task, Unit] =
      // polls kafka and returns sequence of records
    IO(r.consumer.poll(1000).asScala.toSeq)
      // flattens sequence to emit one record per step
      .flatMap(Process emitAll)
      // sends http post requests in parallel 4 at a time
      .gatherMap(4)(sendRecord(r))
      // commits offset of current record in stream once a second
      .evalEvery(1 second)(commitOffset(r.consumer))
      // runs infinitely
      .repeat

  /**
   * This stream runs the pusher stream and handles providing
   * and ensuring the closing of needed resources
   */
  def runWithResources(resources: Task[Resources]) =
    Process.await(resources)(pusher)
           .onComplete(Process eval_ resources.map(_.close()))

  def main(args: Array[String]): Unit = {
    /** 
     * creates stream for each topic/endpoint pair and merges them
     * to run in parallel
     */
    val exec: Task[Unit] =
      for {
        res <- getResourcesList(s"http://${args(0)}/test/config")
        merged <- res.map(r => runWithResources(r))
                     .fold(IO ())((acc, s) => acc merge s).run
      } yield merged

    exec.run
  }
}
