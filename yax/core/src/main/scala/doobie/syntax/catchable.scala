package doobie.syntax

#+scalaz
import scalaz.{ Monad, Catchable, \/, Unapply }
#-scalaz
import doobie.util.{ catchable => C }
#+cats
import cats.Unapply
import scala.{ Either => \/ }
#-cats
#+fs2
import fs2.util.Catchable
#-fs2

/** Syntax for `Catchable` combinators defined in `util.catchable`. */
object catchable {

#+scalaz
  class DoobieCatchableOps[M[_]: Monad, A](self: M[A])(implicit c: Catchable[M]) {
#-scalaz
#+fs2
  class DoobieCatchableOps[M[_], A](self: M[A])(implicit c: Catchable[M]) {
#-fs2

#+cats
    def attempt: M[Throwable \/ A] =
      c.attempt(self)
#-cats

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
#+scalaz
    implicit def toDoobieCatchableOpsUnapply[MA](ma: MA)(
      implicit M0: Unapply[Monad, MA],
               C0: Unapply[Catchable, MA]
    ): DoobieCatchableOps[M0.M, M0.A] =
      new DoobieCatchableOps[M0.M, M0.A](M0.apply(ma))(M0.TC, C0.TC.asInstanceOf[Catchable[M0.M]])
#-scalaz
#+cats
    implicit def toDoobieCatchableOpsUnapply[MA](ma: MA)(
      implicit C0: Unapply[Catchable, MA]
    ): DoobieCatchableOps[C0.M, C0.A] =
      new DoobieCatchableOps[C0.M, C0.A](C0.subst(ma))(C0.TC)
#-cats

  }

  trait ToDoobieCatchableOps extends ToDoobieCatchableOps0 {

    /** @group Syntax */
#+scalaz
    implicit def toDoobieCatchableOps[M[_]: Monad: Catchable, A](ma: M[A]): DoobieCatchableOps[M, A] =
      new DoobieCatchableOps(ma)
#-scalaz
#+cats
    implicit def toDoobieCatchableOps[M[_]: Catchable, A](ma: M[A]): DoobieCatchableOps[M, A] =
      new DoobieCatchableOps(ma)
#-cats

  }

  object ToDoobieCatchableOps extends ToDoobieCatchableOps

}
