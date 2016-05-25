package com.handy.kafka

import org.http4s.Uri
import org.http4s.client.Client
import com.handy.kafka.Consumer._


case class Resources(consumer: Consumer, client: Client, uri: Uri) {
  def close() = {
    consumer.close()
    client.shutdownNow()
  }
}
  
