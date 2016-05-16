package com.handy.kafka

trait Error extends Throwable

case class KafkaSendError(errorMsg: String) extends Error {
  override def getMessage = s"Failed to send message: $errorMsg"
}

case class UriParseError(uri: String) extends Error {
  override def getMessage = "Invalid URI string: $uri"
}
