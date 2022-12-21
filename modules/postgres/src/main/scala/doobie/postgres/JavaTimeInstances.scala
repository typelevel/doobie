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
  implicit val JavaOffsetDateTimeMeta: Meta[java.time.OffsetDateTime] =
    Basic.oneObject(
      JT.TimestampWithTimezone,
      List(JT.Timestamp, JT.TimeWithTimezone),
      classOf[java.time.OffsetDateTime]
    )

  /**
   * This type should map to TIMESTAMP WITH TIMEZONE (TIMESTAMPTZ)
   */
  implicit val JavaInstantMeta: Meta[java.time.Instant] =
    JavaOffsetDateTimeMeta.timap(_.toInstant)(OffsetDateTime.ofInstant(_, ZoneOffset.UTC))

  /**
   * Allows `Timestamp`:
   * https://github.com/pgjdbc/pgjdbc/blob/REL42.4.1/pgjdbc/src/main/java/org/postgresql/jdbc/PgResultSet.java#L732
   */
  implicit val JavaLocalDateMeta: Meta[java.time.LocalDate] =
    Basic.oneObject(
      JT.Date,
      List(JT.Timestamp),
      classOf[java.time.LocalDate]
    )

  implicit val JavaLocalTimeMeta: Meta[java.time.LocalTime] = Meta.JavaLocalTimeMeta

  implicit val JavaLocalDateTimeMeta: Meta[java.time.LocalDateTime] = Meta.JavaLocalDateTimeMeta

  implicit val JavaOffsetTimeMeta: Meta[java.time.OffsetTime] = Meta.JavaOffsetTimeMeta

}
