// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.Applicative
import cats.implicits.*
import doobie.ResultSetIO
import doobie.enumerated.Nullability
import doobie.enumerated.Nullability.{NoNulls, NullabilityKnown}
import doobie.free.resultset as IFRS

import java.sql.ResultSet

// FIXME:
//@implicitNotFound("""
//Cannot find or construct a Read instance for type:
//
//  ${A}
//
//This can happen for a few reasons, but the most common case is that a data
//member somewhere within this type doesn't have a Get instance in scope. Here are
//some debugging hints:
//
//- For auto derivation ensure `doobie.implicits._` or `doobie.generic.auto._` is
//  being imported
//- For Option types, ensure that a Read instance is in scope for the non-Option
//  version.
//- For types you expect to map to a single column ensure that a Get instance is
//  in scope.
//- For case classes, HLists, and shapeless records ensure that each element
//  has a Read instance in scope.
//- Lather, rinse, repeat, recursively until you find the problematic bit.
//
//You can check that an instance exists for Read in the REPL or in your code:
//
//  scala> Read[Foo]
//
//and similarly with Get:
//
//  scala> Get[Foo]
//
//And find the missing instance and construct it as needed. Refer to Chapter 12
//of the book of doobie for more information.
//""")
sealed trait Read[A] {
  def unsafeGet(rs: ResultSet, startIdx: Int): A
  def gets: List[(Get[?], NullabilityKnown)]
  def toOpt: Read[Option[A]]
  def length: Int

  final def get(n: Int): ResultSetIO[A] =
    IFRS.raw(unsafeGet(_, n))

  final def map[B](f: A => B): Read[B] = Read.Composite[B](List(this), l => f(l.head.asInstanceOf[A]))

  final def ap[B](ff: Read[A => B]): Read[B] =
    Read.Composite[B](List(this, ff), l => l(1).asInstanceOf[A => B](l(0).asInstanceOf[A]))
}

object Read extends LowerPriorityRead {

  def apply[A](implicit ev: Read[A]): Read[A] = ev

  def derived[A](implicit ev: MkRead[A]): Read[A] = ev.instance

  trait Auto extends MkReadPlatform

  implicit val ReadApply: Applicative[Read] =
    new Applicative[Read] {
      def ap[A, B](ff: Read[A => B])(fa: Read[A]): Read[B] = fa.ap(ff)
      def pure[A](x: A): Read[A] = Read.Composite(Nil, _ => x)
      override def map[A, B](fa: Read[A])(f: A => B): Read[B] = fa.map(f)
    }

  implicit val unit: Read[Unit] = Read.Composite(Nil, _ => ())

  implicit val optionUnit: Read[Option[Unit]] = Read.Composite(Nil, _ => Some(()))

  implicit def optionalFromRead[A](implicit read: Read[A]): Read[Option[A]] = read.toOpt

  case class Single[A](get: Get[A]) extends Read[A] {
    def unsafeGet(rs: ResultSet, startIdx: Int): A =
      get.unsafeGetNonNullable(rs, startIdx)

    override def toOpt: Read[Option[A]] = SingleOpt(get)

    override def gets: List[(Get[?], NullabilityKnown)] = List(get -> NoNulls)

    override val length: Int = 1

  }

  case class SingleOpt[A](get: Get[A]) extends Read[Option[A]] {
    def unsafeGet(rs: ResultSet, startIdx: Int): Option[A] =
      get.unsafeGetNullable(rs, startIdx)

    override def toOpt: Read[Option[Option[A]]] = Composite(List(this), l => Some(l.head.asInstanceOf[Option[A]]))
    override def gets: List[(Get[?], NullabilityKnown)] = List(get -> Nullability.Nullable)

    override val length: Int = 1
  }

  case class Composite[A](readInstances: List[Read[?]], construct: List[Any] => A) extends Read[A] {

    override val length: Int = readInstances.map(_.length).sum

    override def gets: List[(Get[?], NullabilityKnown)] = readInstances.flatMap(_.gets)

    override def unsafeGet(rs: ResultSet, startIdx: Int): A = {
      import scala.collection.mutable
      val accum = mutable.ArrayBuffer.empty[Any]
      var idx = startIdx
      readInstances.foreach { rr =>
        accum += rr.unsafeGet(rs, idx)
        idx += rr.length
      }
      construct(accum.toList)
    }

    override def toOpt: Read[Option[A]] = {
      val orrs = readInstances.map(_.toOpt)

      val constr: List[Option[Any]] => Option[A] = l =>
        l.sequence.map(construct)

      new Composite[Option[A]](orrs, constr.asInstanceOf[List[Any] => Option[A]])
    }

  }

}

trait LowerPriorityRead extends ReadPlatform {

  implicit def fromGet[A](implicit get: Get[A]): Read[A] = Read.Single(get)

  implicit def fromGetOption[A](implicit get: Get[A]): Read[Option[A]] = Read.SingleOpt(get)

}

final class MkRead[A](val instance: Read[A]) extends AnyVal

object MkRead extends MkReadInstances

trait MkReadInstances extends MkReadPlatform
