package doobie.syntax

import scalaz.{ Monad, Catchable, \/, -\/, \/-, Unapply }
import doobie.util.{ catchable => C }

/** Syntax for `Catchable` combinators defined in `util.catchable`. */
object catchable {

  class DoobieCatchableOps[M[_]: Monad: Catchable, A](self: M[A]) {

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

  trait ToDoobieCatchableOps0 {

    /** @group Syntax */
    implicit def toDoobieCatchableOpsUnapply[MA](ma: MA)(
      implicit M0: Unapply[Monad, MA],
               C0: Unapply[Catchable, MA]
    ): DoobieCatchableOps[M0.M, M0.A] =
      new DoobieCatchableOps[M0.M, M0.A](M0(ma))(M0.TC, C0.TC.asInstanceOf[Catchable[M0.M]])

  }

  trait ToDoobieCatchableOps extends ToDoobieCatchableOps0 {

    /** @group Syntax */
    implicit def toDoobieCatchableOps[M[_]: Monad: Catchable, A](ma: M[A]): DoobieCatchableOps[M, A] =
      new DoobieCatchableOps(ma)

  }

  object ToDoobieCatchableOps extends ToDoobieCatchableOps

}
