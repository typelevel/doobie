package doobie.util

#+scalaz
import scalaz.concurrent.Task
import scalaz.effect.IO
#-scalaz

/** Module for a typeclass for monads with effect-capturing unit. */
object capture {

#+scalaz
  trait Capture[M[_]] {
    def apply[A](a: => A): M[A]
    def delay[A](a: => A): M[A] = apply(a)
  }

  object Capture {

    def apply[M[_]](implicit M: Capture[M]): Capture[M] = M

    implicit val TaskCapture: Capture[Task] =
      new Capture[Task] {
        def apply[A](a: => A): Task[A] =
          Task.delay(a)
      }

    implicit val IOCapture: Capture[IO] =
      new Capture[IO] {
        def apply[A](a: => A): IO[A] =
          IO(a)
      }
  }
#-scalaz
}
