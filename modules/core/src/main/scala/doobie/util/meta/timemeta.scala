// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util.meta

import doobie.enumerated.JdbcType._
import Predef._

trait TimeMeta {

  @deprecated("Use doobie.implicits.javatimedrivernative instead. If you are using a database which doobie directly integrates with, " +
    "You won't need this import anymore as datetime instances are provided in the DB-specific implicit import. " +
    "e.g. for PostgreSQL: `import doobie.postgres.implicits._`. ",
    since = "0.11.0"
  )
  object javatime extends MetaConstructors with TimeMetaInstances

  /**
   * Use this import if you want to use the driver-native support for java.time.* types.
   * This means that the java.time value is passed straight to the JDBC driver you're using
   * without being converted to java.sql.* types (Unlike doobie.implicits.legacy.instant/localdate)
   */
  object javatimedrivernative extends MetaConstructors with TimeMetaInstances

}

trait TimeMetaInstances { this: MetaConstructors =>

  /** @group Instances */
  implicit val JavaTimeInstantMeta: Meta[java.time.Instant] =
    Basic.one[java.time.Instant](
      Timestamp,
      List(Char, VarChar, LongVarChar, Date, Time),
      _.getObject(_, classOf[java.time.Instant]), _.setObject(_, _), _.updateObject(_, _))

  /** @group Instances */
  implicit val JavaTimeLocalDateMeta: Meta[java.time.LocalDate] =
    Basic.one[java.time.LocalDate](
      Date,
      List(Char, VarChar, LongVarChar, Timestamp),
      _.getObject(_, classOf[java.time.LocalDate]), _.setObject(_, _), _.updateObject(_, _))

  /** @group Instances */
  implicit val JavaLocalTimeMeta: Meta[java.time.LocalTime] =
    Basic.one[java.time.LocalTime](
      Time,
      List(Char, VarChar, LongVarChar, Timestamp),
      _.getObject(_, classOf[java.time.LocalTime]), _.setObject(_, _), _.updateObject(_, _))

  /** @group Instances */
  implicit val JavaTimeLocalDateTimeMeta: Meta[java.time.LocalDateTime] =
    Basic.one[java.time.LocalDateTime](
      Timestamp,
      List(Char, VarChar, LongVarChar, Date, Time),
      _.getObject(_, classOf[java.time.LocalDateTime]), _.setObject(_, _), _.updateObject(_, _))

  /** @group Instances */
  implicit val JavaOffsetTimeMeta: Meta[java.time.OffsetTime] =
    Basic.one[java.time.OffsetTime](
      TimeWithTimezone,
      List(Char, VarChar, LongVarChar, Timestamp, Time),
      _.getObject(_, classOf[java.time.OffsetTime]), _.setObject(_, _), _.updateObject(_, _))

  /** @group Instances */
  implicit val JavaOffsetDateTimeMeta: Meta[java.time.OffsetDateTime] =
    Basic.one[java.time.OffsetDateTime](
      TimestampWithTimezone,
      List(Char, VarChar, LongVarChar, Date, Time, Timestamp),
      _.getObject(_, classOf[java.time.OffsetDateTime]), _.setObject(_, _), _.updateObject(_, _))

  /** @group Instances */
  implicit val JavaZonedDateTimeMeta: Meta[java.time.ZonedDateTime] =
    Basic.one[java.time.ZonedDateTime](
      TimestampWithTimezone,
      List(Char, VarChar, LongVarChar, Date, Time, Timestamp),
      _.getObject(_, classOf[java.time.ZonedDateTime]), _.setObject(_, _), _.updateObject(_, _))

}
