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

    def catchSomeLeft[B](p: Throwable => Option[B]): M[B \/ A] =
      C.catchSomeLeft(self)(p)

    def onException[B](action: M[B]): M[A] =
      C.onException(self, action)

    def bracket[B, C](after: A => M[B])(during: A => M[C]): M[C] =
      C.bracket(self)(after)(during)

    def ensuring[B](sequel: M[B]): M[A] =
      C.ensuring(self, sequel)

    def bracket_[B, C](after: M[B])(during: M[C]): M[C] =
      C.bracket_(self)(after)(during)

    def bracketOnError[B, C](after: A => M[B])(during: A => M[C]): M[C] =
      C.bracketOnError(self)(after)(during)

  }

}
