package com.handy.kafka

import scala.concurrent.duration._

import org.log4s._

import org.http4s._
import org.http4s.dsl._
import org.http4s.client.Client

import scalaz._
import Scalaz._
import scalaz.stream._
import scalaz.concurrent.Task
import scalaz.syntax.ToEitherOps

import scodec.bits.ByteVector

import com.handy.kafka.util._
import com.handy.kafka.Consumer.KafkaRecord


object Endpoint {
  val logger = getLogger


  /* logs successful responses and throws on any failed requests */
  private def handleResponse(uri: String)(r: Response) = Task {
    val s = r.status
    if (s.isSuccess) {
      logger.info(s"request to $uri ok with ${s.toString}")
    } else {
      logger.error(s"request to $uri failed")
      throw new KafkaSendError(s.toString)
    }
  }
 
  /* post request task responsible for sending records to service */
  private def send(client: Client, uri: Uri, offset: Long, bv: ByteVector) = {
    logger.info("making request")
    val headers = Headers(Header("Content-Type", "avro/binary"))
    val req = Request(POST, uri, body = Process(bv).toSource, headers = headers)
    client.fetch(req)(handleResponse(uri.toString))
  }

  /**
   * Top level function that gets resources and sends kafka record
   */
  def sendRecord(res: Resources)(rec:KafkaRecord): Task[KafkaRecord] =
    send(
      res.client, res.uri, rec.offset, ByteVector(rec.value)
    ).map(_ => rec)
}
