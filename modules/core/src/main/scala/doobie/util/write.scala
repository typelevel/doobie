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
  val unsafeUpdate: (ResultSet, Int, A) => Unit,
  val unsafeSetOption: (PreparedStatement, Int, Option[A]) => Unit,
  val unsafeUpdateOption: (ResultSet, Int, Option[A]) => Unit,
) { self =>

  lazy val length = puts.length

  def set(n: Int, a: A): PreparedStatementIO[Unit] =
    FPS.raw(unsafeSet(_, n, a))

  def update(n: Int, a: A): ResultSetIO[Unit] =
    FRS.raw(unsafeUpdate(_, n, a))

  def contramap[B](f: B => A): Write[B] =
    new Write(
      puts = puts,
      toList = b => toList(f(b)),
      unsafeSet = (ps, n, a) => unsafeSet(ps, n, f(a)),
      unsafeUpdate = (rs, n, a) => unsafeUpdate(rs, n, f(a)),
      unsafeSetOption = (ps, n, oa) => unsafeSetOption(ps, n, oa.map(f)),
      unsafeUpdateOption = (rs, n, oa) => unsafeUpdateOption(rs, n, oa.map(f)),
    )

  def product[B](fb: Write[B]): Write[(A, B)] =
    new Write[(A, B)](
      puts = puts ++ fb.puts,
      toList = { case (a, b) => toList(a) ++ fb.toList(b) },
      unsafeSet = { case (ps, n, (a, b)) =>
        self.unsafeSet(ps, n, a)
        fb.unsafeSet(ps, n + length, b)
      },
      unsafeUpdate = { case (rs, n, (a, b)) =>
        self.unsafeUpdate(rs, n, a)
        fb.unsafeUpdate(rs, n + length, b)
      },
      unsafeSetOption = { case (ps, n, oab) =>
        self.unsafeSetOption(ps, n, oab.map(_._1))
        fb.unsafeSetOption(ps, n + length, oab.map(_._2))
      },
      unsafeUpdateOption = { case (rs, n, oab) =>
        self.unsafeUpdateOption(rs, n, oab.map(_._1))
        fb.unsafeUpdateOption(rs, n + length, oab.map(_._2))
      },
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
    new Write(
      puts = Nil,
      toList = _ => Nil,
      unsafeSet = (_, _, _) => (),
      unsafeUpdate = (_, _, _) => (),
      unsafeSetOption = (_, _, _) => (),
      unsafeUpdateOption = (_, _, _) => (),
    )

  implicit def fromPut[A](implicit P: Put[A]): Write[A] =
    new Write(
      puts = List((P, NoNulls)),
      toList = a => List(a),
      unsafeSet = (ps, n, a) => P.unsafeSetNonNullable(ps, n, a),
      unsafeUpdate = (rs, n, a) => P.unsafeUpdateNonNullable(rs, n, a),
      unsafeSetOption = (ps, n, oa) => P.unsafeSetNullable(ps, n, oa),
      unsafeUpdateOption = (rs, n, oa) => P.unsafeUpdateNullable(rs, n, oa),
    )

  implicit def fromPutOption[A](implicit P: Put[A]): Write[Option[A]] =
    new Write(
      puts = List((P, Nullable)),
      toList = a => List(a),
      unsafeSet = (ps, n, a) => P.unsafeSetNullable(ps, n, a),
      unsafeUpdate = (rs, n, a) => P.unsafeUpdateNullable(rs, n, a),
      unsafeSetOption = (ps, n, oa) => P.unsafeSetNullable(ps, n, oa.flatten),
      unsafeUpdateOption = (rs, n, oa) => P.unsafeUpdateNullable(rs, n, oa.flatten),
    )
}

sealed trait WriteLowerPriorityImplicits {
  implicit def opt[A](implicit A: Write[A]): Write[Option[A]] = new Write[Option[A]](
    puts = A.puts.map { case (p, _) => (p, Nullable) },
    toList = oa => oa.map(A.toList).getOrElse(A.puts.map(_ => null)),
    unsafeSet = (ps, i, a) => A.unsafeSetOption(ps, i, a),
    unsafeUpdate = (ps, i, a) => A.unsafeUpdateOption(ps, i, a),
    unsafeSetOption = (ps, i, oa) => A.unsafeSetOption(ps, i, oa.flatten),
    unsafeUpdateOption = (ps, i, oa) => A.unsafeUpdateOption(ps, i, oa.flatten),
  )
}
