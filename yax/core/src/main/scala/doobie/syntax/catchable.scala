package doobie.syntax

#+scalaz
import scalaz.{ Monad, Catchable, \/ }
#-scalaz
import doobie.util.{ catchable => C }
#+cats
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
