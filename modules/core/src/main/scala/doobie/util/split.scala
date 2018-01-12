// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.data.NonEmptyList
import cats.free.Coyoneda
import doobie.enum.JdbcType
import java.sql.{ PreparedStatement, ResultSet }

object split {

  // meta is now just a box
  final case class Meta[A](get: Meta.Get[A], put: Meta.Put[A])

  object Meta {

    sealed abstract class Get[A](
      val scalaType: String,
      val jdbcSource: NonEmptyList[JdbcType],
      val get: Coyoneda[(ResultSet, Int) => ?, A]
    ) {

      final def unsafeGetNonNullable(rs: ResultSet, n: Int): A = {
        val i = get.fi(rs, n)
        if (rs.wasNull)
          throw new RuntimeException("invariant violated; null observed in non-null get")
        get.k(i)
      }

      final def unsafeGetNullable(rs: ResultSet, n: Int): Option[A] = {
        val i = get.fi(rs, n)
        if (rs.wasNull) None else Some(get.k(i))
      }

      def map[B](f: A => B): Get[B]

    }

    object Get {

      final case class Basic[A](
        override val scalaType: String,
        override val jdbcSource: NonEmptyList[JdbcType],
                 val jdbcSourceSecondary: List[JdbcType],
        override val get: Coyoneda[(ResultSet, Int) => ?, A]
      ) extends Get[A](scalaType, jdbcSource, get) {
        def map[B](f: A => B): Get[B] = copy(get = get.map(f))
      }

      final case class Advanced[A](
        override val scalaType: String,
        override val jdbcSource: NonEmptyList[JdbcType],
                 val schemaTypes: NonEmptyList[String],
        override val get: Coyoneda[(ResultSet, Int) => ?, A]
      ) extends Get[A](scalaType, jdbcSource, get) {
        def map[B](f: A => B): Get[B] = copy(get = get.map(f))
      }

      /** An implicit Meta[A] means we also have an implicit Get[A]. */
      implicit def metaProjection[A](
        implicit m: Meta[A]
      ): Get[A] =
        m.get

    }

    sealed abstract class Put[A](
      val scalaType: String,
      val jdbcTarget: NonEmptyList[JdbcType],
      val put: ContravariantCoyoneda[(PreparedStatement, Int, ?) => Unit, A],
      val update: ContravariantCoyoneda[(ResultSet, Int, ?) => Unit, A]
    ) {

      @SuppressWarnings(Array("org.wartremover.warts.Equals"))
      def unsafeSetNonNullable(ps: PreparedStatement, n: Int, a: A): Unit =
        if (a == null) sys.error("oops, null")
        else put.fi.apply(ps, n, (put.k(a)))

      def unsafeSetNullable(ps: PreparedStatement, n: Int, oa: Option[A]): Unit =
        oa match {
          case Some(a) => unsafeSetNonNullable(ps, n, a)
          case None    => unsafeSetNull(ps, n)
        }

      @SuppressWarnings(Array("org.wartremover.warts.Equals"))
      def unsafeUpdateNonNullable(rs: ResultSet, n: Int, a: A): Unit =
        if (a == null) sys.error("oops, null")
        else update.fi.apply(rs, n, (update.k(a)))

      def unsafeUpdateNullable(rs: ResultSet, n: Int, oa: Option[A]): Unit =
        oa match {
          case Some(a) => unsafeUpdateNonNullable(rs, n, a)
          case None    => rs.updateNull(n)
        }

      def unsafeSetNull(ps: PreparedStatement, n: Int): Unit

      def contramap[B](f: B => A): Put[B]

    }

    object Put {

      final case class Basic[A](
        override val scalaType: String,
        override val jdbcTarget: NonEmptyList[JdbcType],
        override val put:  ContravariantCoyoneda[(PreparedStatement, Int, ?) => Unit, A],
        override val update: ContravariantCoyoneda[(ResultSet, Int, ?) => Unit, A]
      ) extends Put[A](scalaType, jdbcTarget, put, update) {

        def unsafeSetNull(ps: PreparedStatement, n: Int): Unit =
          ps.setNull(n, jdbcTarget.head.toInt)

        def contramap[B](f: B => A): Put[B] =
          copy(update = update.contramap(f), put = put.contramap(f))

      }

      final case class Advanced[A](
        override val scalaType: String,
        override val jdbcTarget: NonEmptyList[JdbcType],
                 val schemaTypes: NonEmptyList[String],
        override val put:  ContravariantCoyoneda[(PreparedStatement, Int, ?) => Unit, A],
        override val update: ContravariantCoyoneda[(ResultSet, Int, ?) => Unit, A]
      ) extends Put[A](scalaType, jdbcTarget, put, update) {

        def unsafeSetNull(ps: PreparedStatement, n: Int): Unit =
          ps.setNull(n, jdbcTarget.head.toInt, schemaTypes.head)

        def contramap[B](f: B => A): Put[B] =
          copy(update = update.contramap(f), put = put.contramap(f))

      }

      /** An implicit Meta[A] means we also have an implicit Put[A]. */
      implicit def metaProjectionWrite[A](
        implicit m: Meta[A]
      ): Put[A] =
        m.put

    }

  }

}