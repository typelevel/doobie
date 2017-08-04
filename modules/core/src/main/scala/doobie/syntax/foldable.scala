package doobie.syntax

import doobie.util.{ foldable => F }

import scalaz._, Scalaz._

/** Module of additional functions for `Foldable`. */
object foldable {

  class DoobieFoldableOps[F[_]: Foldable, A](self: F[A]) {

    def foldSmash(prefix: A, delim: A, suffix: A)(implicit ev: Monoid[A]): A =
      F.foldSmash(self)(prefix, delim, suffix)

    def foldSmash1(prefix: A, delim: A, suffix: A)(implicit A: Monoid[A]): A =
      F.foldSmash1(self)(prefix, delim, suffix)

  }

  trait ToDoobieFoldableOps {
    implicit def toDoobieFoldableOps[F[_]: Foldable, A](fa: F[A]): DoobieFoldableOps[F, A] =
      new DoobieFoldableOps(fa)
  }

}
