// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.ContravariantSemigroupal
import doobie.enumerated.Nullability
import doobie.enumerated.Nullability.*
import doobie.free.{PreparedStatementIO, ResultSetIO}

import java.sql.{PreparedStatement, ResultSet}
import doobie.util.fragment.Fragment
import doobie.util.fragment.Elem
import doobie.free.{preparedstatement as IFPS, resultset as IFRS}

import scala.annotation.implicitNotFound

@implicitNotFound("""
Cannot find or construct a Write instance for type:

  ${A}

This can happen for a few reasons, but the most common case is that a data
member somewhere within this type doesn't have a Put instance in scope. Here are
some debugging hints:

- For Option types, ensure that a Write instance is in scope for the non-Option
  version.
- For types you expect to map to a single column ensure that a Put instance is
  in scope.
- For case classes, shapeless HLists/records ensure that each element
  has a Write instance in scope.
- Lather, rinse, repeat, recursively until you find the problematic bit.

You can check that an instance exists for Write in the REPL or in your code:

  scala> Write[Foo]

and similarly with Put:

  scala> Put[Foo]

And find the missing instance and construct it as needed. Refer to Chapter 12
of the book of doobie for more information.
""")
trait Write[A] {
  def puts: List[(Put[?], NullabilityKnown)]
  def toList(a: A): List[Any]
  def unsafeSet(ps: PreparedStatement, startIdx: Int, a: A): Unit
  def unsafeUpdate(rs: ResultSet, startIdx: Int, a: A): Unit
  def toOpt: Write[Option[A]]
  def length: Int

  final def set(n: Int, a: A): PreparedStatementIO[Unit] =
    IFPS.raw(unsafeSet(_, n, a))

  final def update(n: Int, a: A): ResultSetIO[Unit] =
    IFRS.raw(unsafeUpdate(_, n, a))

  final def contramap[B](f: B => A): Write[B] = new Write.Composite[B](List(this), b => List(f(b)))

  final def product[B](fb: Write[B]): Write[(A, B)] = {
    new Write.Composite[(A, B)](List(this, fb), tuple => List(tuple._1, tuple._2))
  }

  def toFragment(a: A, sql: String = List.fill(length)("?").mkString(",")): Fragment = {
    val elems: List[Elem] = (puts zip toList(a)).map {
      case ((p, nullab), x) => nullab match {
          case NoNulls  => p match { case px: Put[x] => Elem.Arg(x.asInstanceOf[x], px) }
          case Nullable => p match { case px: Put[x] => Elem.Opt(x.asInstanceOf[Option[x]], px) }
        }
    }
    Fragment(sql, elems, None)
  }
}

object Write extends WritePlatform {
  def apply[A](implicit A: Write[A]): Write[A] = A

  implicit val WriteContravariantSemigroupal: ContravariantSemigroupal[Write] =
    new ContravariantSemigroupal[Write] {
      def contramap[A, B](fa: Write[A])(f: B => A): Write[B] = fa.contramap(f)
      def product[A, B](fa: Write[A], fb: Write[B]): Write[(A, B)] = fa.product(fb)
    }

  implicit val unitWrite: Write[Unit] =
    new Composite[Unit](Nil, _ => List.empty)

  /** Simple instance wrapping a Put. i.e. single column non-null value */
  class Single[A](put: Put[A]) extends Write[A] {
    override val length: Int = 1

    override def unsafeSet(ps: PreparedStatement, startIdx: Int, a: A): Unit =
      put.unsafeSetNonNullable(ps, startIdx, a)

    override def unsafeUpdate(rs: ResultSet, startIdx: Int, a: A): Unit =
      put.unsafeUpdateNonNullable(rs, startIdx, a)

    override def puts: List[(Put[?], NullabilityKnown)] = List(put -> Nullability.NoNulls)

    override def toList(a: A): List[Any] = List(a)

    override def toOpt: Write[Option[A]] = new SingleOpt(put)
  }

  /** Simple instance wrapping a Put. i.e. single column nullable value */
  class SingleOpt[A](put: Put[A]) extends Write[Option[A]] {
    override val length: Int = 1

    override def unsafeSet(ps: PreparedStatement, startIdx: Int, a: Option[A]): Unit =
      put.unsafeSetNullable(ps, startIdx, a)

    override def unsafeUpdate(rs: ResultSet, startIdx: Int, a: Option[A]): Unit =
      put.unsafeUpdateNullable(rs, startIdx, a)

    override def puts: List[(Put[?], NullabilityKnown)] = List(put -> Nullability.Nullable)

    override def toList(a: Option[A]): List[Any] = List(a)

    override def toOpt: Write[Option[Option[A]]] = new Composite[Option[Option[A]]](List(this), x => List(x.flatten))
  }

  /** A Write instance consists of multiple underlying Write instances */
  class Composite[A](
      writeInstances: List[Write[?]],
      deconstruct: A => List[Any]
  ) extends Write[A] {
    override lazy val length: Int = writeInstances.map(_.length).sum

    // Make the types match up with deconstruct
    private val anyWrites: List[Write[Any]] = writeInstances.asInstanceOf[List[Write[Any]]]

    override def unsafeSet(ps: PreparedStatement, startIdx: Int, a: A): Unit = {
      val parts = deconstruct(a)
      var idx = startIdx
      anyWrites.zip(parts).foreach { case (w, p) =>
        w.unsafeSet(ps, idx, p)
        idx += w.length
      }
    }

    override def unsafeUpdate(rs: ResultSet, startIdx: Int, a: A): Unit = {
      val parts = deconstruct(a)
      var idx = startIdx
      anyWrites.zip(parts).foreach { case (w, p) =>
        w.unsafeUpdate(rs, idx, p)
        idx += w.length
      }
    }

    override lazy val puts: List[(Put[?], NullabilityKnown)] = writeInstances.flatMap(_.puts)

    override def toList(a: A): List[Any] =
      anyWrites.zip(deconstruct(a)).flatMap { case (w, p) => w.toList(p) }

    override def toOpt: Write[Option[A]] = new Composite[Option[A]](
      writeInstances.map(_.toOpt),
      {
        case Some(a) => deconstruct(a).map(Some(_))
        case None    => List.fill(writeInstances.length)(None) // All Nones
      }
    )
  }
}
