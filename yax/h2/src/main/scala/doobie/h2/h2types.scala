package doobie.h2

import doobie.enum.jdbctype
import doobie.util.meta._
import doobie.util.invariant._

import java.util.UUID

import scala.Predef._
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag

/** `Meta` instances for H2 types. */
object H2Types extends H2Types

trait H2Types {

  implicit val UuidType = Meta.other[UUID]("uuid")

  // see postgres contrib for an explanation of array mapping; we may want to factor this out

  private def boxedPair[A >: Null <: AnyRef: ClassTag: TypeTag]: (Meta[Array[A]], Meta[Array[Option[A]]]) = {
    val raw = Meta.other[Array[Object]]("ARRAY").xmap[Array[A]](
      a => if (a == null) null else a.map(_.asInstanceOf[A]),
      a => if (a == null) null else a.map(_.asInstanceOf[Object]))
    def checkNull[B >: Null](a: Array[B], e: Exception): Array[B] =
      if (a == null) null else if (a.exists(_ == null)) throw e else a
    (raw.xmap(checkNull(_, NullableCellRead), checkNull(_, NullableCellUpdate)),
     raw.xmap[Array[Option[A]]](_.map(Option(_)), _.map(_.orNull).toArray))
  }

  implicit val (unliftedBooleanArrayType, liftedBooleanArrayType) = boxedPair[java.lang.Boolean]
  implicit val (unliftedIntegerArrayType, liftedIntegerArrayType) = boxedPair[java.lang.Integer]
  implicit val (unliftedLongArrayType,    liftedLongArrayType)    = boxedPair[java.lang.Long]
  implicit val (unliftedFloatArrayType,   liftedFloatArrayType)   = boxedPair[java.lang.Float]
  implicit val (unliftedDoubleArrayType,  liftedDoubleArrayType)  = boxedPair[java.lang.Double]
  implicit val (unliftedStringArrayType,  liftedStringArrayType)  = boxedPair[java.lang.String]

  private def unboxedPair[A >: Null <: AnyRef: ClassTag, B <: AnyVal: ClassTag: TypeTag](f: A => B, g: B => A)(
    implicit boxed: Meta[Array[A]], boxedLifted: Meta[Array[Option[A]]]): (Meta[Array[B]], Meta[Array[Option[B]]]) =
    (boxed.xmap(a => if (a == null) null else a.map(f), a => if (a == null) null else a.map(g)),
     boxedLifted.xmap(_.asInstanceOf[Array[Option[B]]], _.asInstanceOf[Array[Option[A]]]))

  implicit val (unliftedUnboxedBooleanArrayType, liftedUnboxedBooleanArrayType) = unboxedPair[java.lang.Boolean, scala.Boolean](_.booleanValue, java.lang.Boolean.valueOf)
  implicit val (unliftedUnboxedIntegerArrayType, liftedUnboxedIntegerArrayType) = unboxedPair[java.lang.Integer, scala.Int]    (_.intValue,     java.lang.Integer.valueOf)
  implicit val (unliftedUnboxedLongArrayType,    liftedUnboxedLongArrayType)    = unboxedPair[java.lang.Long,    scala.Long]   (_.longValue,    java.lang.Long.valueOf)
  implicit val (unliftedUnboxedFloatArrayType,   liftedUnboxedFloatArrayType)   = unboxedPair[java.lang.Float,   scala.Float]  (_.floatValue,   java.lang.Float.valueOf)
  implicit val (unliftedUnboxedDoubleArrayType,  liftedUnboxedDoubleArrayType)  = unboxedPair[java.lang.Double,  scala.Double] (_.doubleValue,  java.lang.Double.valueOf)

}
