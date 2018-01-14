// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.h2

import doobie.enum.JdbcType
import doobie.util.Meta
import doobie.util.invariant._

import java.util.UUID

import scala.Predef._
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag

import cats.data.NonEmptyList.{ of => NonEmptyListOf }

@SuppressWarnings(Array("org.wartremover.warts.ExplicitImplicitTypes"))
trait Instances {

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit val UuidType =
    Meta.advanced[UUID](
      NonEmptyListOf(JdbcType.Binary),
      NonEmptyListOf("uuid", "UUID"),
      _.getObject(_) match {
        case null => null
        case uuidObj =>
          try uuidObj.asInstanceOf[UUID]
          catch {
            case _: ClassCastException => throw InvalidObjectMapping(classOf[UUID], uuidObj.getClass)
          }
      },
      (ps, n, a) => ps.setObject(n, a),
      (rs, n, a) => rs.updateObject(n, a)
    )

  // see postgres contrib for an explanation of array mapping; we may want to factor this out

  @SuppressWarnings(Array("org.wartremover.warts.Equals", "org.wartremover.warts.AsInstanceOf"))
  private def boxedPair[A >: Null <: AnyRef: ClassTag: TypeTag]: (Meta[Array[A]], Meta[Array[Option[A]]]) = {
    val raw = Meta.other[Array[Object]]("ARRAY").timap[Array[A]](
      a => if (a == null) null else a.map(_.asInstanceOf[A]))(
      a => if (a == null) null else a.map(_.asInstanceOf[Object]))
    def checkNull[B >: Null](a: Array[B], e: Exception): Array[B] =
      if (a == null) null else if (a.exists(_ == null)) throw e else a
    (raw.timap(checkNull(_, NullableCellRead))(checkNull(_, NullableCellUpdate)),
     raw.timap[Array[Option[A]]](_.map(Option(_)))(_.map(_.orNull).toArray))
  }

  implicit val (unliftedBooleanArrayType, liftedBooleanArrayType) = boxedPair[java.lang.Boolean]
  implicit val (unliftedIntegerArrayType, liftedIntegerArrayType) = boxedPair[java.lang.Integer]
  implicit val (unliftedLongArrayType,    liftedLongArrayType)    = boxedPair[java.lang.Long]
  implicit val (unliftedFloatArrayType,   liftedFloatArrayType)   = boxedPair[java.lang.Float]
  implicit val (unliftedDoubleArrayType,  liftedDoubleArrayType)  = boxedPair[java.lang.Double]
  implicit val (unliftedStringArrayType,  liftedStringArrayType)  = boxedPair[java.lang.String]

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private def unboxedPair[A >: Null <: AnyRef: ClassTag, B <: AnyVal: ClassTag: TypeTag](f: A => B, g: B => A)(
    implicit boxed: Meta[Array[A]], boxedLifted: Meta[Array[Option[A]]]): (Meta[Array[B]], Meta[Array[Option[B]]]) =
    (boxed.timap(a => if (a == null) null else a.map(f))(a => if (a == null) null else a.map(g)),
     boxedLifted.timap(_.asInstanceOf[Array[Option[B]]])(_.asInstanceOf[Array[Option[A]]]))

  implicit val (unliftedUnboxedBooleanArrayType, liftedUnboxedBooleanArrayType) = unboxedPair[java.lang.Boolean, scala.Boolean](_.booleanValue, java.lang.Boolean.valueOf)
  implicit val (unliftedUnboxedIntegerArrayType, liftedUnboxedIntegerArrayType) = unboxedPair[java.lang.Integer, scala.Int]    (_.intValue,     java.lang.Integer.valueOf)
  implicit val (unliftedUnboxedLongArrayType,    liftedUnboxedLongArrayType)    = unboxedPair[java.lang.Long,    scala.Long]   (_.longValue,    java.lang.Long.valueOf)
  implicit val (unliftedUnboxedFloatArrayType,   liftedUnboxedFloatArrayType)   = unboxedPair[java.lang.Float,   scala.Float]  (_.floatValue,   java.lang.Float.valueOf)
  implicit val (unliftedUnboxedDoubleArrayType,  liftedUnboxedDoubleArrayType)  = unboxedPair[java.lang.Double,  scala.Double] (_.doubleValue,  java.lang.Double.valueOf)

}
