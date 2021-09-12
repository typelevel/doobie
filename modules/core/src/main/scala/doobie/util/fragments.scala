// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie
package util

import cats.data.NonEmptyList
import cats.syntax.all._
import cats.{Foldable, Reducible}
import doobie.implicits._

/** Module of `Fragment` constructors. */
object fragments {

  /** Returns `VALUES fs0, fs1, ...`. */
  def values[F[_]: Reducible, A](fs: F[A])(implicit w: util.Write[A]): Fragment =
    fs.toList.map(a => fr0"${w.toFragment(a)}").foldSmash1(fr0"VALUES ", fr",", fr"")

  /** Returns `(f IN (fs0, fs1, ...))`. */
  def in[A: util.Put](f: Fragment, fs0: A, fs1: A, fs: A*): Fragment =
    in(f, fs0 :: fs1 :: fs.toList)

  /** Returns `(f IN (fs0, fs1, ...))`, or `false` for empty `fs`. */
  def in[F[_]: Foldable, A: util.Put](f: Fragment, fs: F[A]): Fragment = fs.toList.toNel match {
    case Some(fs) => parentheses(f ++ fr"IN" ++ parentheses(comma(fs.map(a => fr"$a"))))
    case None => fr"${false}"
  }

  /** Returns `(f IN ((fs0-A, fs0-B), (fs1-A, fs1-B), ...))`, or `false` for empty `fs`. */
  def in[F[_]: Foldable, A: util.Put, B: util.Put](f: Fragment, fs: F[(A,B)]): Fragment = fs.toList.toNel match {
    case Some(fs) => parentheses(f ++ fr"IN" ++ parentheses(comma(fs.map { case (a,b) => fr0"($a,$b)" })))
    case None => fr"${false}"
  }

  /** Returns `(f NOT IN (fs0, fs1, ...))`. */
  def notIn[A: util.Put](f: Fragment, fs0: A, fs1: A, fs: A*): Fragment =
    notIn(f, fs0 :: fs1 :: fs.toList)

  /** Returns `(f NOT IN (fs0, fs1, ...))`, or `true` for empty `fs`. */
  def notIn[F[_]: Foldable, A: util.Put](f: Fragment, fs: F[A]): Fragment = fs.toList.toNel match {
    case Some(fs) => parentheses(f ++ fr"NOT IN" ++ parentheses(comma(fs.map(a => fr"$a"))))
    case None => fr"${true}"
  }

  /** Returns `(f1 AND f2 AND ... fn)`. */
  def and(f1: Fragment, f2: Fragment, fs: Fragment*): Fragment =
    and(f1 :: f2 :: fs.toList)

  /** Returns `(f1 AND f2 AND ... fn)`, or `true` for empty `fs`. */
  def and[F[_]: Foldable](fs: F[Fragment]): Fragment = fs.toList.toNel match {
    case Some(fs) => parentheses(fs.intercalate(fr"AND"))
    case None => fr"${true}"
  }

  /** Returns `(f1 AND f2 AND ... fn)` for all defined fragments. */
  def andOpt(fs: Option[Fragment]*): Fragment =
    and(fs.toList.unite)

  /** Returns `(f1 OR f2 OR ... fn)`. */
  def or(f1: Fragment, f2: Fragment, fs: Fragment*): Fragment =
    or(f1 :: f2 :: fs.toList)

  /** Returns `(f1 OR f2 OR ... fn)`, or `true` for empty `fs`. */
  def or[F[_]: Foldable](fs: F[Fragment]): Fragment = fs.toList.toNel match {
    case Some(fs) => parentheses(fs.intercalate(fr"OR"))
    case None => fr"${false}"
  }

  /** Returns `(f1 OR f2 OR ... fn)` for all defined fragments. */
  def orOpt(fs: Option[Fragment]*): Fragment =
    or(fs.toList.unite)

  /** Returns `WHERE f1 AND f2 AND ... fn`. */
  def whereAnd(f1: Fragment, fs: Fragment*): Fragment =
    whereAnd(f1 :: fs.toList)

  /** Returns `WHERE f1 AND f2 AND ... fn` or the empty fragment if `fs` is empty. */
  def whereAnd[F[_]: Foldable](fs: F[Fragment]): Fragment =
    if (fs.isEmpty) Fragment.empty else fr"WHERE" ++ and(fs)

  /** Returns `WHERE f1 AND f2 AND ... fn` for defined `f`, if any, otherwise the empty fragment. */
  def whereAndOpt(fs: Option[Fragment]*): Fragment =
    whereAnd(fs.toList.unite)

  /** Returns `WHERE f1 OR f2 OR ... fn`. */
  def whereOr(f1: Fragment, fs: Fragment*): Fragment =
    whereOr(NonEmptyList(f1, fs.toList))

  /** Returns `WHERE f1 OR f2 OR ... fn` or the empty fragment if `fs` is empty. */
  def whereOr[F[_]: Foldable](fs: F[Fragment]): Fragment =
    if (fs.isEmpty) Fragment.empty else fr"WHERE" ++ or(fs)

  /** Returns `WHERE f1 OR f2 OR ... fn` for defined `f`, if any, otherwise the empty fragment. */
  def whereOrOpt(fs: Option[Fragment]*): Fragment =
    whereOr(fs.toList.unite)

  /** Returns `SET f1, f2, ... fn`. */
  def set(f1: Fragment, fs: Fragment*): Fragment =
    set(NonEmptyList(f1, fs.toList))

  /** Returns `SET f1, f2, ... fn`. */
  def set[F[_]: Reducible](fs: F[Fragment]): Fragment =
    fr"SET" ++ comma(fs)

  /** Returns `(f)`. */
  def parentheses(f: Fragment): Fragment = fr0"(" ++ f ++ fr")"

  /** Returns `?,?,...,?` for the values in `a`. */
  def values[A](a: A)(implicit w: util.Write[A]): Fragment =
    w.toFragment(a)

  /** Returns `f1, f2, ... fn`. */
  def comma(f1: Fragment, f2: Fragment, fs: Fragment*): Fragment =
    comma(NonEmptyList(f1, f2 :: fs.toList))

  /** Returns `f1, f2, ... fn`. */
  def comma[F[_]: Reducible](fs: F[Fragment]): Fragment =
    fs.intercalate(fr",")

  /** Returns `ORDER BY f1, f2, ... fn`. */
  def orderBy(f1: Fragment, fs: Fragment*): Fragment =
    comma(NonEmptyList(f1, fs.toList))

  /** Returns `ORDER BY f1, f2, ... fn` or the empty fragment if `fs` is empty. */
  def orderBy[F[_]: Foldable](fs: F[Fragment]): Fragment = fs.toList.toNel match {
    case Some(fs) => fr"ORDER BY" ++ comma(fs)
    case None => Fragment.empty
  }

  /** Returns `ORDER BY f1, f2, ... fn` for defined `f`, if any, otherwise the empty fragment. */
  def orderByOpt(fs: Option[Fragment]*): Fragment =
    orderBy(fs.toList.unite)
}
