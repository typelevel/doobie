package doobie.util

/** Module for a typeclass for monads with effect-capturing unit. */
object capture {

  trait Capture[M[_]] {
    def apply[A](a: => A): M[A]
  }

  object Capture {
    def apply[M[_]](implicit M: Capture[M]): Capture[M] = M
  }

}

