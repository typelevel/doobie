// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.Contravariant
import cats.data.NonEmptyList
import doobie.enum.JdbcType
import java.sql.{ PreparedStatement, ResultSet }
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.{ Type, TypeTag }

sealed abstract class Put[A](
  val typeStack: NonEmptyList[Option[Type]],
  val jdbcTargets: NonEmptyList[JdbcType],
  val put: ContravariantCoyoneda[(PreparedStatement, Int, ?) => Unit, A],
  val update: ContravariantCoyoneda[(ResultSet, Int, ?) => Unit, A]
) {

  protected def contramapImpl[B](f: B => A, typ: Option[Type]): Put[B]

  def unsafeSetNull(ps: PreparedStatement, n: Int): Unit

  final def contramap[B](f: B => A): Put[B] =
    contramapImpl(f, None)

  final def tcontramap[B](f: B => A)(implicit ev: TypeTag[B]): Put[B] =
    contramapImpl(f, Some(ev.tpe))

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

}

object Put extends PutInstances {

  def apply[A](implicit ev: Put[A]): ev.type = ev

  final case class Basic[A](
    override val typeStack: NonEmptyList[Option[Type]],
    override val jdbcTargets: NonEmptyList[JdbcType],
    override val put:  ContravariantCoyoneda[(PreparedStatement, Int, ?) => Unit, A],
    override val update: ContravariantCoyoneda[(ResultSet, Int, ?) => Unit, A]
  ) extends Put[A](typeStack, jdbcTargets, put, update) {

    protected def contramapImpl[B](f: B => A, typ: Option[Type]): Put[B] =
      copy(typeStack = typ :: typeStack, update = update.contramap(f), put = put.contramap(f))

    def unsafeSetNull(ps: PreparedStatement, n: Int): Unit =
      ps.setNull(n, jdbcTargets.head.toInt)

  }

  object Basic {

    def many[A](
      jdbcTargets: NonEmptyList[JdbcType],
      put:  (PreparedStatement, Int, A) => Unit,
      update: (ResultSet, Int, A) => Unit
    )(implicit ev: TypeTag[A]): Basic[A] =
      Basic(
        NonEmptyList.of(Some(ev.tpe)),
        jdbcTargets,
        ContravariantCoyoneda.lift[(PreparedStatement, Int, ?) => Unit, A](put),
        ContravariantCoyoneda.lift[(ResultSet, Int, ?) => Unit, A](update)
      )

  }

  final case class Advanced[A](
    override val typeStack: NonEmptyList[Option[Type]],
    override val jdbcTargets: NonEmptyList[JdbcType],
             val schemaTypes: NonEmptyList[String],
    override val put:  ContravariantCoyoneda[(PreparedStatement, Int, ?) => Unit, A],
    override val update: ContravariantCoyoneda[(ResultSet, Int, ?) => Unit, A]
  ) extends Put[A](typeStack, jdbcTargets, put, update) {

    protected def contramapImpl[B](f: B => A, typ: Option[Type]): Put[B] =
      copy(typeStack = typ :: typeStack, update = update.contramap(f), put = put.contramap(f))

    def unsafeSetNull(ps: PreparedStatement, n: Int): Unit =
      ps.setNull(n, jdbcTargets.head.toInt, schemaTypes.head)

  }
  object Advanced {

    def many[A](
      jdbcTargets: NonEmptyList[JdbcType],
      schemaTypes: NonEmptyList[String],
      put:  (PreparedStatement, Int, A) => Unit,
      update: (ResultSet, Int, A) => Unit
    )(implicit ev: TypeTag[A]): Advanced[A] =
      Advanced(
        NonEmptyList.of(Some(ev.tpe)),
        jdbcTargets,
        schemaTypes,
        ContravariantCoyoneda.lift[(PreparedStatement, Int, ?) => Unit, A](put),
        ContravariantCoyoneda.lift[(ResultSet, Int, ?) => Unit, A](update)
      )

    def one[A: TypeTag](
      jdbcTarget: JdbcType,
      schemaTypes: NonEmptyList[String],
      put:  (PreparedStatement, Int, A) => Unit,
      update: (ResultSet, Int, A) => Unit
    ): Advanced[A] =
      many(NonEmptyList.of(jdbcTarget), schemaTypes, put, update)

    @SuppressWarnings(Array("org.wartremover.warts.Equals", "org.wartremover.warts.AsInstanceOf"))
    def array[A >: Null <: AnyRef: TypeTag](
      schemaTypes: NonEmptyList[String],
      elementType: String
    ): Advanced[Array[A]] =
      one(
        JdbcType.Array,
        schemaTypes,
        (ps, n, a) => {
          val conn = ps.getConnection
          val arr  = conn.createArrayOf(elementType, a.asInstanceOf[Array[AnyRef]])
          ps.setArray(n, arr)
        },
        (rs, n, a) => {
          val stmt = rs.getStatement
          val conn = stmt.getConnection
          val arr  = conn.createArrayOf(elementType, a.asInstanceOf[Array[AnyRef]])
          rs.updateArray(n, arr)
        }
      )

    def other[A >: Null <: AnyRef: TypeTag](schemaTypes: NonEmptyList[String]): Advanced[A] =
      many(
        NonEmptyList.of(JdbcType.Other, JdbcType.JavaObject),
        schemaTypes,
        (ps, n, a) => ps.setObject(n, a),
        (rs, n, a) => rs.updateObject(n, a)
      )

  }

  /** An implicit Meta[A] means we also have an implicit Put[A]. */
  implicit def metaProjectionWrite[A](
    implicit m: Meta[A]
  ): Put[A] =
    m.put

}

trait PutInstances {

  /** @group Instances */
  implicit val ContravariantPut: Contravariant[Put] =
    new Contravariant[Put] {
      def contramap[A, B](fa: Put[A])(f: B => A): Put[B] =
        fa.contramap(f)
    }

  /** @group Instances */
  implicit def ArrayTypeAsListGet[A: ClassTag: TypeTag](implicit ev: Put[Array[A]]): Put[List[A]] =
    ev.tcontramap(_.toArray)

  /** @group Instances */
  implicit def ArrayTypeAsVectorGet[A: ClassTag: TypeTag](implicit ev: Put[Array[A]]): Put[Vector[A]] =
    ev.tcontramap(_.toArray)

}