package com.handy.kafka

import scala.concurrent.duration._

import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.blaze._
import org.http4s.Status.NotFound
import org.http4s.Status.ResponseClass.Successful

import io.circe._
import io.circe.generic.auto._
import org.http4s.circe.jsonOf

import scalaz._
import Scalaz._
import scalaz.concurrent.Task

import com.handy.kafka.util._
import com.handy.kafka.Consumer._


/**
 * This code is responsible for retrieving the configuration parameters
 * from the service that will be receiving events
 */
object Initializer {
  val clientConfig = 
    BlazeClientConfig.defaultConfig.copy(requestTimeout = 20 seconds)

  def getClient = Task.delay(PooledHttp1Client(10, clientConfig))


  case class Config(topic: String, path: String, groupId: String)
  implicit val topicUriDecoder = jsonOf[List[Config]]

  /* makes get request to retrieve configuration params */
  private def getConfigs(configUri: Uri): Task[List[Config]] =
    getClient.flatMap(client => 
      client.get(configUri) {
        case Successful(resp) => resp.as[List[Config]]
        case _ => Task.delay(throw ConfigUriError(configUri.path))
      }
    )

  /* initializes resouces (creates client and subscribes kafka consumer) */
  private def resList(configs: List[Config]): List[Task[Resources]] =
    configs.map(c =>
      for {
        consumer <- subscribe(c.topic, c.groupId) 
        uri <- Task.delay(uriFromString(c.path).valueOr(throw _))
        client <- getClient
      } yield Resources(consumer, client, uri)
    )

  /** 
   * Top level function to create task for retreiving resources needed to
   * run pusher streams.
   */
  def getResourcesList(configPath: String): Task[List[Task[Resources]]] = 
    for {
      uri <-  Task.delay(uriFromString(configPath).valueOr(throw _))
      configs <- getConfigs(uri)
    } yield resList(configs)
}
