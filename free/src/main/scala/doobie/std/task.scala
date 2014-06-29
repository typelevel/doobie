package doobie.std

import doobie.util.capture.Capture
import scalaz.concurrent.Task

/** Module of typeclass instances for `scalaz.concurrent.Task`. */
object task {
  
  implicit val TaskCapture: Capture[Task] =
    new Capture[Task] {
      def apply[A](a: => A): Task[A] =
        Task.delay(a)
    }

}