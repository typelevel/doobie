package doobie.util

import cats._, cats.implicits._

/** Module of additional functions for `Foldable`. */
object foldable {

  /** Generalization of `mkString` for any monoid. */
  def foldSmash[F[_]: Foldable, A](fa: F[A])(prefix: A, delim: A, suffix: A)(implicit ev: Monoid[A]): A =
    ev.combine(prefix, ev.combine(fa.intercalate(delim), suffix))

  /** Like `foldSmash` but returns monoidal zero if the foldable is empty. */
  def foldSmash1[F[_]: Foldable, A](fa: F[A])(prefix: A, delim: A, suffix: A)(implicit A: Monoid[A]): A =
    if (fa.isEmpty) A.empty else foldSmash(fa)(prefix, delim, suffix)

}
