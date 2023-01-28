// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import doobie.Meta
import doobie.enumerated.{JdbcType => JT}
import doobie.util.meta.MetaConstructors

import java.time.{OffsetDateTime, ZoneOffset} // Using database JDBC driver native support

/**
 * Instances for JSR-310 date time types.
 *
 * Implementation is based on https://jdbc.postgresql.org/documentation/head/java8-date-time.html, using
 * native support for Postgres JDBC driver.
 */
trait JavaTimeInstances extends MetaConstructors {

  /**
   * This type should map to TIMESTAMP WITH TIMEZONE (TIMESTAMPTZ)
   * When writing to the database, the same instant is preserved if your target column is of type TIMESTAMPTZ
   * (The JDBC driver works out the timezone conversion for you). Note that since offset information is not stored in
   * the database column, retrieving the same value will yield the same instant in time, but with offset = 0 (UTC)
   */
  implicit val JavaTimeOffsetDateTimeMeta: Meta[java.time.OffsetDateTime] =
    Basic.one[java.time.OffsetDateTime](
      JT.TimestampWithTimezone,
      List(JT.Timestamp, JT.TimeWithTimezone),
      _.getObject(_, classOf[java.time.OffsetDateTime]), _.setObject(_, _), _.updateObject(_, _))

  /**
   * This type should map to TIMESTAMP WITH TIMEZONE (TIMESTAMPTZ)
   */
  implicit val JavaTimeInstantMeta: Meta[java.time.Instant] =
    JavaTimeOffsetDateTimeMeta.timap(_.toInstant)(OffsetDateTime.ofInstant(_, ZoneOffset.UTC))

  /**
   * This type should map to TIMESTAMP
   */
  implicit val JavaTimeLocalDateTimeMeta: Meta[java.time.LocalDateTime] =
    Basic.one[java.time.LocalDateTime](
      JT.Timestamp,
      Nil,
      _.getObject(_, classOf[java.time.LocalDateTime]), _.setObject(_, _), _.updateObject(_, _))

  /**
   * This type should map to DATE
   */
  implicit val JavaTimeLocalDateMeta: Meta[java.time.LocalDate] =
    Basic.one[java.time.LocalDate](
      JT.Date,
      List(JT.Timestamp),
      _.getObject(_, classOf[java.time.LocalDate]), _.setObject(_, _), _.updateObject(_, _))

  /**
   * This type should map to TIME
   */
  implicit val JavaTimeLocalTimeMeta: Meta[java.time.LocalTime] =
    Basic.one[java.time.LocalTime](
      JT.Time,
      Nil,
      _.getObject(_, classOf[java.time.LocalTime]), _.setObject(_, _), _.updateObject(_, _))

  /**
   * This type should map to TIME WITH TIMEZONE (TIMETZ)
   */
  implicit val JavaTimeOffsetTimeMeta: Meta[java.time.OffsetTime] =
    Basic.one[java.time.OffsetTime](
      JT.TimeWithTimezone,
      Nil,
      _.getObject(_, classOf[java.time.OffsetTime]), _.setObject(_, _), _.updateObject(_, _))

}
