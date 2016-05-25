package com.handy.kafka

import org.http4s._
import scalaz._, Scalaz._
import scalaz.stream._
import scalaz.concurrent.Task

package object util {
  def uriFromString(uri: String): UriParseError \/ Uri =
    Uri.fromString(uri).fold(_ => UriParseError(uri).left, identity(_).right)

  def IO[A](a: => A): Process[Task, A] = Process.eval(Task.delay(a))
}

