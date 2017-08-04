package doobie.syntax

import scalaz.{ Monad, Catchable, \/ }
import doobie.util.{ catchable => C }

/** Syntax for `Catchable` combinators defined in `util.catchable`. */
object catchable {

  class DoobieCatchableOps[M[_]: Monad, A](self: M[A])(implicit c: Catchable[M]) {

    def attemptSome[B](handler: PartialFunction[Throwable, B]): M[B \/ A] =
      C.attemptSome(self)(handler)

    def except(handler: Throwable => M[A]): M[A] =
      C.except(self)(handler)

    def exceptSome(handler: PartialFunction[Throwable, M[A]]): M[A] =
      C.exceptSome(self)(handler)

    def onException[B](action: M[B]): M[A] =
      C.onException(self)(action)

    def ensuring[B](sequel: M[B]): M[A] =
      C.ensuring(self)(sequel)

  }

  trait ToDoobieCatchableOps {

    /** @group Syntax */
    implicit def toDoobieCatchableOps[M[_]: Monad: Catchable, A](ma: M[A]): DoobieCatchableOps[M, A] =
      new DoobieCatchableOps(ma)

  }

  object ToDoobieCatchableOps extends ToDoobieCatchableOps

}
