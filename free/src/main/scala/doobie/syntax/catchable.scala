package doobie.syntax

import scalaz.{ Monad, Catchable, \/, -\/, \/- }
import doobie.util.{ catchable => C }

/** Syntax for `Catchable` combinators defined in `util.catchable`. */
object catchable {

  implicit class DoobieCatchableOps[M[_]: Monad: Catchable, A](self: M[A]) {

    def except(handler: Throwable => M[A]): M[A] =
      C.except(self)(handler)

    def catchSome[B](p: Throwable => Option[B], handler: B => M[A]): M[A] =
      C.catchSome(self)(p, handler)

    def catchLeft: M[Throwable \/ A] =
      C.catchLeft(self)

  }

}
