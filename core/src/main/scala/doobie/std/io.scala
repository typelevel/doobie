package doobie.std

import doobie.util.capture.Capture
import scalaz.effect.IO

/** Module of typeclass instances for `scalaz.effect.IO`. */
object io {
  
  implicit val IOCapture: Capture[IO] =
    new Capture[IO] {
      def apply[A](a: => A): IO[A] =
        IO(a)
    }

}