package com.handy.kafka.syntax

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

import org.specs2._
import org.specs2.mock.Mockito

import scalaz.stream._
import scalaz.concurrent.Task

class ProcessSyntaxSpec extends Specification with Mockito { def is = s2"""
  sleepAfterEach should
  run the function f on the value f after sleeping                      $t1
  interleave the values of f with the process its called on             $t2
  sleep for at least the provided durations when run                    $t3

  evalEvery should
  call the function once every duration period                          $t4
  """
 
  def t1 = { 
    var count = 0
    Process.constant(()).sleepAfterEach(3 millis, 2 millis, 1 millis) {
      _ => count += 1
    }.take(6).run.run
    count mustEqual 3
  }

  def t2 = Process(1, 3, 5).sleepAfterEach(2 millis, 4 millis, 6 millis) (
      d => d.length.toInt
    ).runLog.run mustEqual Vector(1, 2, 3, 4, 5, 6)

  def t3 = {
    val t1 = System.currentTimeMillis()
    Process.constant(()).sleepAfterEach(10 millis, 20 millis, 30 millis)(_ => ()).take(6).run.run
    val t2 = System.currentTimeMillis()
    val diff = t2 - t1
    diff.must(be_>=(60.toLong))
  }

  def t4 = {
    var count = 0
    Process.repeatEval(Task(Thread.sleep(100))).evalEvery(450 millis, _ =>
      Task { count += 1; count }
    ).take(10).runLog.run
    count mustEqual 2
  }
}
