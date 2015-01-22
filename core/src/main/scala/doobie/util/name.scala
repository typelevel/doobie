package doobie.util

import doobie.util.capture.Capture
import scalaz.{ Name, Catchable, \/ }

/** Typeclass instances for scalaz.Name */
object name {

  implicit val NameCatchable: Catchable[Name] =
    new Catchable[Name] {
      def attempt[A](fa: Name[A]): Name[Throwable \/ A] =
        Name(try \/.right(fa.value) catch { case t: Throwable => \/.left(t) })
      def fail[A](err: Throwable): Name[A] =
        Name(throw err)
    }

  implicit val NameCapture: Capture[Name] =
    new Capture[Name] {
      def apply[A](a: => A) = Name(a)
    }

}

