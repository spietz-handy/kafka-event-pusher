package com.handy.kafka

import scala.concurrent.duration._

import org.http4s._
import org.http4s.dsl._
import org.http4s.client.Client

import scalaz._
import Scalaz._
import scalaz.stream._
import scalaz.concurrent.Task
import scalaz.syntax.ToEitherOps

import scodec.bits.ByteVector

import EventConsumer.KafkaRecord


object HttpSender {
  def uriFromString(uri: String): UriParseError \/ Uri =
    Uri.fromString(uri).fold(_ => UriParseError(uri).left, identity(_).right)

  def handleResponse(uri: String)(r: Response) = Task {
    val s = r.status
    if (s.isSuccess) {
      println(s"request completed ${s.toString}")
    } else {
      println(s"request to $uri failed: ${s.toString}")
      //throw new KafkaSendError(s.toString)
    }
  }
  
  def send(client: Client, uri: Uri, offset: Long, bv: ByteVector): Task[Unit] = {
    println("making request")
    val headers = Headers(Header("Content-Type", "avro/binary"))
    val req = Request(POST, uri, body = Process(bv).toSource, headers = headers)
    client.fetch(req)(handleResponse(uri.toString))
  }

  def sendRecord(rec: KafkaRecord, res: Resources, retryDurs: List[Duration]) = 
    send(
      res.client, res.url, rec.offset, ByteVector(rec.value)
    )//.retry(retryDurs)

}
