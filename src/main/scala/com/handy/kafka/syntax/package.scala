package com.handy.kafka

import scala.concurrent.duration.Duration

import scalaz.concurrent.Task
import scalaz.stream.time._
import scalaz.stream.Process
import scalaz.stream.Process.constant


package object syntax {
  implicit class ProcessSyntax[A](p: Process[Task, A]) {
    /** 
     * This will interleave a sleep task in between each step in a stream
     *repeating the last duration infinitely.
     */
    def sleepAfterEach(durs: Duration*)(f: Duration => A): Process[Task, A] = {
      val dursP = (Process(durs: _*) ++ constant(durs.last)).toSource
      p interleave dursP.flatMap (d =>
        Process eval Task {
          Thread.sleep(d.toMillis)
          f(d)
        }
      )
    }

    def evalEvery(d: Duration)(f: A => Task[Unit]): Process[Task, Unit] =
      p.zip(every(d)).flatMap { case (a, b) => 
        Process eval (if (b) f(a) else Task.now(()))
      }
  }
}


