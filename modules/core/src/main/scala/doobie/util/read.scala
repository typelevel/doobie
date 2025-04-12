// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.Applicative
import doobie.ResultSetIO
import doobie.enumerated.Nullability
import doobie.enumerated.Nullability.{NoNulls, NullabilityKnown}
import doobie.free.resultset as IFRS

import java.sql.ResultSet
import scala.annotation.implicitNotFound
import scala.reflect.ClassTag

@implicitNotFound("""
Cannot find or construct a Read instance for type:

  ${A}

This can happen for a few reasons, but the most common case is that a data
member somewhere within this type doesn't have a Get instance in scope. Here are
some debugging hints:

- For auto derivation ensure `doobie.implicits._` or `doobie.generic.auto._` is
  being imported
- For Option types, ensure that a Read instance is in scope for the non-Option
  version.
- For types you expect to map to a single column ensure that a Get instance is
  in scope.
- For case classes, shapeless HLists/records ensure that each element
  has a Read instance in scope.
- Lather, rinse, repeat, recursively until you find the problematic bit.

You can check that an instance exists for Read in the REPL or in your code:

  scala> Read[Foo]

and similarly with Get:

  scala> Get[Foo]

And find the missing instance and construct it as needed. Refer to Chapter 12
of the book of doobie for more information.
""")
trait Read[A] {
  def unsafeGet(rs: ResultSet, startIdx: Int): A
  def gets: List[(Get[?], NullabilityKnown)]
  def toOpt: Read[Option[A]]
  def length: Int

  final def get(n: Int): ResultSetIO[A] =
    IFRS.raw(unsafeGet(_, n))

  final def map[B](f: A => B): Read[B] = new Read.Transform[B, A](this, f)

  final def ap[B](ff: Read[A => B]): Read[B] = {
    new Read.Composite[B, A => B, A](ff, this, (f, a) => f(a))
  }
}

object Read extends ReadPlatform {

  def apply[A](implicit ev: Read[A]): Read[A] = ev

  implicit val ReadApply: Applicative[Read] =
    new Applicative[Read] {
      def ap[A, B](ff: Read[A => B])(fa: Read[A]): Read[B] = fa.ap(ff)
      def pure[A](x: A): Read[A] = unitRead.map(_ => x)
      override def map[A, B](fa: Read[A])(f: A => B): Read[B] = fa.map(f)
    }

  implicit val unitRead: Read[Unit] = new Read[Unit] {
    override def unsafeGet(rs: ResultSet, startIdx: Int): Unit = {
      () // Does not read anything from ResultSet
    }
    override def gets: List[(Get[?], NullabilityKnown)] = List.empty
    override def toOpt: Read[Option[Unit]] = this.map(_ => Some(()))
    override def length: Int = 0
  }

  /** Simple instance wrapping a Get. i.e. single column non-null value */
  class Single[A](get: Get[A]) extends Read[A] {
    def unsafeGet(rs: ResultSet, startIdx: Int): A =
      get.unsafeGetNonNullable(rs, startIdx)

    override def toOpt: Read[Option[A]] = new SingleOpt(get)

    override def gets: List[(Get[?], NullabilityKnown)] = List(get -> NoNulls)

    override val length: Int = 1

  }

  /** Simple instance wrapping a Get. i.e. single column nullable value */
  class SingleOpt[A](get: Get[A]) extends Read[Option[A]] {
    def unsafeGet(rs: ResultSet, startIdx: Int): Option[A] =
      get.unsafeGetNullable(rs, startIdx)

    override def toOpt: Read[Option[Option[A]]] = new Transform[Option[Option[A]], Option[A]](this, a => Some(a))
    override def gets: List[(Get[?], NullabilityKnown)] = List(get -> Nullability.Nullable)

    override val length: Int = 1
  }

  class Transform[A, From](underlyingRead: Read[From], f: From => A) extends Read[A] {
    override def unsafeGet(rs: ResultSet, startIdx: Int): A = f(underlyingRead.unsafeGet(rs, startIdx))
    override def gets: List[(Get[?], NullabilityKnown)] = underlyingRead.gets
    override def toOpt: Read[Option[A]] =
      new Transform[Option[A], Option[From]](underlyingRead.toOpt, opt => opt.map(f))
    override lazy val length: Int = underlyingRead.length
  }

  /** A Read instance consists of two underlying Read instances */
  class Composite[A, S0, S1](read0: Read[S0], read1: Read[S1], f: (S0, S1) => A) extends Read[A] {
    override def unsafeGet(rs: ResultSet, startIdx: Int): A = {
      val r0 = read0.unsafeGet(rs, startIdx)
      val r1 = read1.unsafeGet(rs, startIdx + read0.length)
      f(r0, r1)
    }

    override lazy val gets: List[(Get[?], NullabilityKnown)] =
      read0.gets ++ read1.gets

    override def toOpt: Read[Option[A]] = {
      val readOpt0 = read0.toOpt
      val readOpt1 = read1.toOpt
      new Composite[Option[A], Option[S0], Option[S1]](
        readOpt0,
        readOpt1,
        {
          case (Some(s0), Some(s1)) => Some(f(s0, s1))
          case _                    => None
        })

    }
    override lazy val length: Int = read0.length + read1.length
  }

  /** A Composite made up of a list of underlying Read instances. This class but is intended to provide a simpler
    * interface for other libraries that uses Doobie. This isn't used by Doobie itself for its derived instances.
    *
    * For large number of columns, this class may be more performant than chain of Read.Composite.
    */
  class CompositeOfInstances[A: ClassTag](readInstances: Array[Read[A]]) extends Read[Array[A]] {
    override def unsafeGet(rs: ResultSet, startIdx: Int): Array[A] = {
      var columnIdx = startIdx
      readInstances.map { r =>
        val res = r.unsafeGet(rs, columnIdx)
        columnIdx += r.length // This Read instance "consumed" x number of columns
        res
      }
    }

    override def gets: List[(Get[?], NullabilityKnown)] = readInstances.flatMap(_.gets).toList

    override def toOpt: Read[Option[Array[A]]] = {
      new CompositeOfInstances(readInstances.map(_.toOpt)).map(arraySequence)
    }

    override def length: Int = readInstances.map(_.length).sum
  }

}
