// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.ContravariantSemigroupal
import doobie.enumerated.Nullability._
import doobie.free.{ FPS, FRS, PreparedStatementIO, ResultSetIO }
import java.sql.{ PreparedStatement, ResultSet }
import doobie.util.fragment.Fragment
import doobie.util.fragment.Elem

final class Write[A](
  val puts: List[(Put[_], NullabilityKnown)],
  val toList: A => List[Any],
  val unsafeSet: (PreparedStatement, Int, A) => Unit,
  val unsafeUpdate: (ResultSet, Int, A) => Unit
) {

  lazy val length = puts.length

  def set(n: Int, a: A): PreparedStatementIO[Unit] =
    FPS.raw(unsafeSet(_, n, a))

  def update(n: Int, a: A): ResultSetIO[Unit] =
    FRS.raw(unsafeUpdate(_, n, a))

  def contramap[B](f: B => A): Write[B] =
    new Write(
      puts,
      b => toList(f(b)),
      (ps, n, a) => unsafeSet(ps, n, f(a)),
      (rs, n, a) => unsafeUpdate(rs, n, f(a))
    )

  def product[B](fb: Write[B]): Write[(A, B)] =
    new Write(
      puts ++ fb.puts,
      { case (a, b) => toList(a) ++ fb.toList(b) },
      { case (ps, n, (a, b)) => unsafeSet(ps, n, a); fb.unsafeSet(ps, n + length, b) },
      { case (rs, n, (a, b)) => unsafeUpdate(rs, n, a); fb.unsafeUpdate(rs, n + length, b) }
    )

  /**
   * Given a value of type `A` and an appropriately parameterized SQL string we can construct a
   * `Fragment`. If `sql` is unspecified a comma-separated list of `length` placeholders will be used.
   */
  def toFragment(a: A, sql: String = List.fill(length)("?").mkString(",")): Fragment = {
    val elems: List[Elem] = (puts zip toList(a)).map {
      case ((p: Put[a], NoNulls), a) => Elem.Arg(a.asInstanceOf[a], p)
      case ((p: Put[a], Nullable), a) => Elem.Opt(a.asInstanceOf[Option[a]], p)
    }
    Fragment(sql, elems, None)
  }

}

object Write extends WritePlatform with WriteLowerPriorityImplicits {

  def apply[A](implicit A: Write[A]): Write[A] = A

  implicit val ReadContravariantSemigroupal: ContravariantSemigroupal[Write] =
    new ContravariantSemigroupal[Write] {
      def contramap[A, B](fa: Write[A])(f: B => A) = fa.contramap(f)
      def product[A, B](fa: Write[A], fb: Write[B]) = fa.product(fb)
    }

  implicit val unitComposite: Write[Unit] =
    new Write(Nil, _ => Nil, (_, _, _) => (), (_, _, _) => ())

  implicit def fromPut[A](implicit P: Put[A]): Write[A] =
    new Write(
      List((P, NoNulls)),
      a => List(a),
      (ps, n, a) => P.unsafeSetNonNullable(ps, n, a),
      (rs, n, a) => P.unsafeUpdateNonNullable(rs, n, a)
    )

  implicit def fromPutOption[A](implicit P: Put[A]): Write[Option[A]] =
    new Write(
      List((P, Nullable)),
      a => List(a),
      (ps, n, a) => P.unsafeSetNullable(ps, n, a),
      (rs, n, a) => P.unsafeUpdateNullable(rs, n, a)
    )

}

sealed trait WriteLowerPriorityImplicits {
  implicit val ohnil: Write[Option[Unit]] =
    new Write[Option[Unit]](Nil, _ => Nil, (_, _, _) => (), (_, _, _) => ())

  implicit def opt[A](implicit A: Write[A]): Write[Option[A]] = new Write[Option[A]](
    A.puts.map {case (p, _) => (p, Nullable)},
    oa => oa.map(A.toList).getOrElse(A.puts.map(_ => null)),
    (ps, i, oa) => oa.foreach(a => A.unsafeSet(ps, i, a)),
    (ps, i, oa) => oa.foreach(a => A.unsafeUpdate(ps, i, a))
  )
}