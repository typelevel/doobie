package doobie.util

#+scalaz
import scalaz._, Scalaz._
#-scalaz
#+cats
import cats._, cats.implicits._
#-cats

/** Module of additional functions for `Foldable`. */
object foldable {

  /** Generalization of `mkString` for any monoid. */
  def foldSmash[F[_]: Foldable, A](fa: F[A])(prefix: A, delim: A, suffix: A)(implicit ev: Monoid[A]): A =
#+scalaz
    ev.append(prefix, ev.append(fa.intercalate(delim), suffix))
#-scalaz
#+cats
    ev.combine(prefix, ev.combine(fa.intercalate(delim), suffix))
#-cats

  /** Like `foldSmash` but returns monoidal zero if the foldable is empty. */
  def foldSmash1[F[_]: Foldable, A](fa: F[A])(prefix: A, delim: A, suffix: A)(implicit A: Monoid[A]): A =
#+scalaz
    if (fa.empty)   A.zero  else foldSmash(fa)(prefix, delim, suffix)
#-scalaz
#+cats
    if (fa.isEmpty) A.empty else foldSmash(fa)(prefix, delim, suffix)
#-cats

}
