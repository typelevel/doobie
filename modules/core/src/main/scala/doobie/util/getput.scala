// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.Invariant
import cats.data.NonEmptyList
import doobie.enum.JdbcType
import java.sql.{ PreparedStatement, ResultSet }
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag

object meta {

  sealed abstract case class Meta[A] private (get: Get[A], put: Put[A]) {

    def imap[B](f: A => B)(g: B => A): Meta[B] =
      new Meta(get.map(f), put.contramap(g)) {}

    def timap[B: TypeTag](f: A => B)(g: B => A): Meta[B] =
      new Meta(get.tmap(f), put.tcontramap(g)) {}

    @deprecated("Will be removed. Use Put#jdbcTargets", "0.5.0")
    def jdbcTarget: NonEmptyList[JdbcType] =
      put.jdbcTargets

    @deprecated("Will be removed. Use Get#jdbcSources", "0.5.0")
    def jdbcSource: NonEmptyList[JdbcType] =
      get.jdbcSources

    @deprecated("Will be removed. Use Get/Put#typeStack.head.toString", "0.5.0")
    @SuppressWarnings(Array("org.wartremover.warts.ToString"))
    def scalaType: String =
      get.typeStack.head.toString

  }

  object Meta extends GetPutInstances {

    def apply[A](implicit ev: Meta[A]): ev.type = ev

    def basic[A: TypeTag](
      jdbcTarget: NonEmptyList[JdbcType],
      jdbcSource: NonEmptyList[JdbcType],
      jdbcSourceSecondary: List[JdbcType],
      get: (ResultSet, Int) => A,
      put: (PreparedStatement, Int, A) => Unit,
      update: (ResultSet, Int, A) => Unit
    ): Meta[A] =
      new Meta(
        Get.Basic.many(jdbcSource, jdbcSourceSecondary, get),
        Put.Basic.many(jdbcTarget, put, update)
      ) {}

    def basic1[A: TypeTag](
      jdbcType: JdbcType,
      jdbcSourceSecondary: List[JdbcType],
      get: (ResultSet, Int) => A,
      put: (PreparedStatement, Int, A) => Unit,
      update: (ResultSet, Int, A) => Unit
    ): Meta[A] =
      new Meta(
        Get.Basic.many(NonEmptyList.of(jdbcType), jdbcSourceSecondary, get),
        Put.Basic.many(NonEmptyList.of(jdbcType), put, update)
      ) {}

    def advanced[A: TypeTag](
      jdbcTypes: NonEmptyList[JdbcType],
      schemaTypes: NonEmptyList[String],
      get: (ResultSet, Int) => A,
      put: (PreparedStatement, Int, A) => Unit,
      update: (ResultSet, Int, A) => Unit
    ): Meta[A] =
      new Meta(
        Get.Advanced.many(jdbcTypes, schemaTypes, get),
        Put.Advanced.many(jdbcTypes, schemaTypes, put, update)
      ) {}

    def array[A >: Null <: AnyRef: TypeTag](
      elementType: String,
      schemaH: String,
      schemaT: String*
    ): Meta[Array[A]] =
      new Meta[Array[A]](
        Get.Advanced.array[A](NonEmptyList(schemaH, schemaT.toList)),
        Put.Advanced.array[A](NonEmptyList(schemaH, schemaT.toList), elementType)
      ) {}

    def other[A >: Null <: AnyRef: TypeTag: ClassTag](
      schemaH: String,
      schemaT: String*
    ): Meta[A] =
      new Meta(
        Get.Advanced.other[A](NonEmptyList(schemaH, schemaT.toList)),
        Put.Advanced.other[A](NonEmptyList(schemaH, schemaT.toList))
      ) {}

  }

  trait GetPutInstances { this: Meta.type =>
    import doobie.enum.JdbcType.{ Boolean => JdbcBoolean, _ }

    /** @group Instances */
    implicit val GetPutInvariant: Invariant[Meta] =
      new Invariant[Meta] {
        def imap[A, B](fa: Meta[A])(f: A => B)(g: B => A): Meta[B] =
          fa.imap(f)(g)
      }

    /** @group Instances */
    implicit val ByteMeta: Meta[Byte] =
      basic1[Byte](
        TinyInt,
        List(SmallInt, Integer, BigInt, Real, Float, Double, Decimal, Numeric, Bit, Char, VarChar,
          LongVarChar),
        _.getByte(_), _.setByte(_, _), _.updateByte(_, _))

    /** @group Instances */
    implicit val ShortMeta: Meta[Short] =
      basic1[Short](
        SmallInt,
        List(TinyInt, Integer, BigInt, Real, Float, Double, Decimal, Numeric, Bit, Char, VarChar,
          LongVarChar),
        _.getShort(_), _.setShort(_, _), _.updateShort(_, _))

    /** @group Instances */
    implicit val IntMeta: Meta[Int] =
      basic1[Int](
        Integer,
        List(TinyInt, SmallInt, BigInt, Real, Float, Double, Decimal, Numeric, Bit, Char, VarChar,
          LongVarChar),
        _.getInt(_), _.setInt(_, _), _.updateInt(_, _))

    /** @group Instances */
    implicit val LongMeta: Meta[Long] =
      basic1[Long](
        BigInt,
        List(TinyInt, Integer, SmallInt, Real, Float, Double, Decimal, Numeric, Bit, Char, VarChar,
          LongVarChar),
        _.getLong(_), _.setLong(_, _), _.updateLong(_, _))

    /** @group Instances */
    implicit val FloatMeta: Meta[Float] =
      basic1[Float](
        Real,
        List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Decimal, Numeric, Bit, Char, VarChar,
          LongVarChar),
        _.getFloat(_), _.setFloat(_, _), _.updateFloat(_, _))

    /** @group Instances */
    implicit val DoubleMeta: Meta[Double] =
      basic[Double](
        NonEmptyList.of(Double),
        NonEmptyList.of(Float, Double),
        List(TinyInt, Integer, SmallInt, BigInt, Float, Real, Decimal, Numeric, Bit, Char, VarChar,
          LongVarChar),
        _.getDouble(_), _.setDouble(_, _), _.updateDouble(_, _))

    /** @group Instances */
    implicit val BigDecimalMeta: Meta[java.math.BigDecimal] =
      basic[java.math.BigDecimal](
        NonEmptyList.of(Numeric),
        NonEmptyList.of(Decimal, Numeric),
        List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Real, Bit, Char, VarChar,
          LongVarChar),
        _.getBigDecimal(_), _.setBigDecimal(_, _), _.updateBigDecimal(_, _))

    /** @group Instances */
    implicit val BooleanMeta: Meta[Boolean] =
      basic[Boolean](
        NonEmptyList.of(Bit, JdbcBoolean),
        NonEmptyList.of(Bit, JdbcBoolean),
        List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Real, Decimal, Numeric, Char, VarChar,
          LongVarChar),
        _.getBoolean(_), _.setBoolean(_, _), _.updateBoolean(_, _))

    /** @group Instances */
    implicit val StringMeta: Meta[String] =
      basic[String](
        NonEmptyList.of(VarChar, Char, LongVarChar, NChar, NVarChar, LongnVarChar),
        NonEmptyList.of(Char, VarChar, LongVarChar, NChar, NVarChar, LongnVarChar),
        List(TinyInt, Integer, SmallInt, BigInt, Float, Double, Real, Decimal, Numeric, Bit,
          Binary, VarBinary, LongVarBinary, Date, Time, Timestamp),
        _.getString(_), _.setString(_, _), _.updateString(_, _))

    /** @group Instances */
    implicit val ByteArrayMeta: Meta[Array[Byte]] =
      basic[Array[Byte]](
        NonEmptyList.of(Binary, VarBinary, LongVarBinary),
        NonEmptyList.of(Binary, VarBinary),
        List(LongVarBinary),
        _.getBytes(_), _.setBytes(_, _), _.updateBytes(_, _))

    /** @group Instances */
    implicit val DateMeta: Meta[java.sql.Date] =
      basic1[java.sql.Date](
        Date,
        List(Char, VarChar, LongVarChar, Timestamp),
        _.getDate(_), _.setDate(_, _), _.updateDate(_, _))

    /** @group Instances */
    implicit val TimeMeta: Meta[java.sql.Time] =
      basic1[java.sql.Time](
        Time,
        List(Char, VarChar, LongVarChar, Timestamp),
        _.getTime(_), _.setTime(_, _), _.updateTime(_, _))

    /** @group Instances */
    implicit val TimestampMeta: Meta[java.sql.Timestamp] =
      basic1[java.sql.Timestamp](
        Timestamp,
        List(Char, VarChar, LongVarChar, Date, Time),
        _.getTimestamp(_), _.setTimestamp(_, _), _.updateTimestamp(_, _))

    /** @group Instances */
    implicit val ScalaBigDecimalMeta: Meta[BigDecimal] =
      BigDecimalMeta.imap(BigDecimal.apply)(_.bigDecimal)

    /** @group Instances */
    implicit val JavaUtilDateMeta: Meta[java.util.Date] =
      DateMeta.imap[java.util.Date](a => a)(d => new java.sql.Date(d.getTime))

    /** @group Instances */
    implicit val JavaTimeInstantMeta: Meta[java.time.Instant] =
      TimestampMeta.imap(_.toInstant)(java.sql.Timestamp.from)

    /** @group Instances */
    implicit val JavaTimeLocalDateMeta: Meta[java.time.LocalDate] =
      DateMeta.imap(_.toLocalDate)(java.sql.Date.valueOf)

  }

}