// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import doobie.Meta
import doobie.enumerated.{JdbcType => JT}
import doobie.util.meta.MetaConstructors

import java.time.{OffsetDateTime, ZoneOffset}

/**
 * Instances for JSR-310 date time types.
 *
 * Implementation is based on https://jdbc.postgresql.org/documentation/head/java8-date-time.html, using
 * native support for Postgres JDBC driver.
 */
trait JavaTimeInstances extends MetaConstructors {

  /**
   * This type should map to TIMESTAMP WITH TIMEZONE (TIMESTAMPTZ).
   *
   * Allows `TimeWithTimezone` and `Timestamp` reluctantly. See comment in the driver:
   * https://github.com/pgjdbc/pgjdbc/blob/REL42.4.1/pgjdbc/src/main/java/org/postgresql/jdbc/PgResultSet.java#L645
   */
  implicit val JavaTimeOffsetDateTimeMeta: Meta[java.time.OffsetDateTime] =
    Basic.oneObject(
      JT.TimestampWithTimezone,
      List(JT.Timestamp, JT.TimeWithTimezone),
      classOf[java.time.OffsetDateTime]
    )

  /**
   * This type should map to TIMESTAMP WITH TIMEZONE (TIMESTAMPTZ)
   */
  implicit val JavaTimeInstantMeta: Meta[java.time.Instant] =
    JavaTimeOffsetDateTimeMeta.timap(_.toInstant)(OffsetDateTime.ofInstant(_, ZoneOffset.UTC))

  /**
   * This type should map to TIMESTAMP WITH TIMEZONE (TIMESTAMPTZ)
   */
  implicit val JavaTimeZonedDateTimeMeta: Meta[java.time.ZonedDateTime] =
    JavaTimeOffsetDateTimeMeta.timap(_.atZoneSameInstant(ZoneOffset.UTC))(_.toOffsetDateTime)

  /**
   * Allows `Timestamp`:
   * https://github.com/pgjdbc/pgjdbc/blob/REL42.4.1/pgjdbc/src/main/java/org/postgresql/jdbc/PgResultSet.java#L732
   */
  implicit val JavaTimeLocalDateMeta: Meta[java.time.LocalDate] =
    Basic.oneObject(
      JT.Date,
      List(JT.Timestamp),
      classOf[java.time.LocalDate]
    )
}
