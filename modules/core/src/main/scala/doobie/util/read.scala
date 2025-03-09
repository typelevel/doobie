// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats._
import doobie.free.{FRS, ResultSetIO}
import doobie.enumerated.Nullability._
import java.sql.ResultSet
import scala.annotation.implicitNotFound

@implicitNotFound("...")
sealed abstract class Read[A](
    val gets: List[(Get[_], NullabilityKnown)],
    val unsafeGet: (ResultSet, Int) => A
) {
  final lazy val length: Int = gets.length

  def map[B](f: A => B): Read[B] =
    Read.of(gets, (rs, n) => f(unsafeGet(rs, n)))

  def ap[B](ff: Read[A => B]): Read[B] =
    Read.of(ff.gets ++ gets, (rs, n) => ff.unsafeGet(rs, n)(unsafeGet(rs, n + ff.length)))

  def get(n: Int): ResultSetIO[A] =
    FRS.raw(unsafeGet(_, n))
}

object Read {

  // Named case class replacing anonymous subclasses
  final case class BasicRead[A](
      override val gets: List[(Get[_], NullabilityKnown)],
      override val unsafeGet: (ResultSet, Int) => A
  ) extends Read[A](gets, unsafeGet)

  // Factory method
  def of[A](
      gets: List[(Get[_], NullabilityKnown)],
      unsafeGet: (ResultSet, Int) => A
  ): Read[A] = BasicRead(gets, unsafeGet)

  def apply[A](implicit ev: Read[A]): ev.type = ev

  def derived[A](implicit ev: MkRead[A]): Read[A] = ev

  trait Auto {
    implicit def deriveRead[A](implicit ev: MkRead[A]): Read[A] = ev
  }

  implicit val ReadApply: Applicative[Read] =
    new Applicative[Read] {
      def ap[A, B](ff: Read[A => B])(fa: Read[A]): Read[B] = fa.ap(ff)
      def pure[A](x: A): Read[A] = of(Nil, (_, _) => x)
      override def map[A, B](fa: Read[A])(f: A => B): Read[B] = fa.map(f)
    }

  implicit val unit: Read[Unit] =
    of(Nil, (_, _) => ())

  implicit def fromGet[A](implicit ev: Get[A]): Read[A] =
    of(List((ev, NoNulls)), ev.unsafeGetNonNullable)

  implicit def fromGetOption[A](implicit ev: Get[A]): Read[Option[A]] =
    of(List((ev, Nullable)), ev.unsafeGetNullable)

  // Optimized instances starting around line 90
  implicit def readOption[A](implicit ev: Read[A]): Read[Option[A]] =
    of(
      ev.gets.map { case (g, n) => (g, Nullable) },
      (rs, n) => if (rs.getObject(n) == null) None else Some(ev.unsafeGet(rs, n))
    )

  implicit def readTuple2[A, B](implicit A: Read[A], B: Read[B]): Read[(A, B)] =
    of(
      A.gets ++ B.gets,
      (rs, n) => (A.unsafeGet(rs, n), B.unsafeGet(rs, n + A.length))
    )

  implicit def readTuple3[A, B, C](implicit A: Read[A], B: Read[B], C: Read[C]): Read[(A, B, C)] =
    of(
      A.gets ++ B.gets ++ C.gets,
      (rs, n) =>
        (
          A.unsafeGet(rs, n),
          B.unsafeGet(rs, n + A.length),
          C.unsafeGet(rs, n + A.length + B.length)
        )
    )

  // Add more tuple instances (up to Tuple22) and other types like Either as needed
}

final class MkRead[A](
    override val gets: List[(Get[_], NullabilityKnown)],
    override val unsafeGet: (ResultSet, Int) => A
) extends Read[A](gets, unsafeGet)

object MkRead extends ReadPlatform {
  def lift[A](r: Read[A]): MkRead[A] =
    new MkRead[A](r.gets, r.unsafeGet)
}
