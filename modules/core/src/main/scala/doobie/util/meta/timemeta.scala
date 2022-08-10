// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util.meta

import doobie.enumerated.JdbcType._

/**
 * Strict instances for Java time classes that only allow an exact match.
 *
 * A driver may provide more lenient instances.
 * e.g. for PostgreSQL: `import doobie.postgres.implicits._`
 */
trait TimeMetaInstances { this: MetaConstructors =>
  import Predef.classOf

  /** @group Instances */
  implicit val JavaTimeLocalDateMeta: Meta[java.time.LocalDate] =
    Basic.oneObject(
      Date,
      Nil,
      classOf[java.time.LocalDate]
    )

  /** @group Instances */
  implicit val JavaLocalTimeMeta: Meta[java.time.LocalTime] =
    Basic.oneObject(
      Time,
      Nil,
      classOf[java.time.LocalTime]
    )

  /** @group Instances */
  implicit val JavaTimeLocalDateTimeMeta: Meta[java.time.LocalDateTime] =
    Basic.oneObject(
      Timestamp,
      Nil,
      classOf[java.time.LocalDateTime]
    )

  /** @group Instances */
  implicit val JavaOffsetTimeMeta: Meta[java.time.OffsetTime] =
    Basic.oneObject(
      TimeWithTimezone,
      Nil,
      classOf[java.time.OffsetTime]
    )

  /** @group Instances */
  implicit val JavaOffsetDateTimeMeta: Meta[java.time.OffsetDateTime] =
    Basic.oneObject(
      TimestampWithTimezone,
      List(TimeWithTimezone),
      classOf[java.time.OffsetDateTime]
    )

  // extra instances not in the spec

  /** @group Instances */
  implicit val JavaTimeInstantMeta: Meta[java.time.Instant] =
    JavaOffsetDateTimeMeta.imap(_.toInstant)(_.atOffset(java.time.ZoneOffset.UTC))

  /** @group Instances */
  implicit val JavaTimeZoneId: Meta[java.time.ZoneId] = {
    def parse(str: String) = try {
      Right(java.time.ZoneId.of(str))
    } catch {
      case e: java.time.DateTimeException => Left(e.getMessage)
    }

    Meta[String].tiemap(parse(_))(_.getId)
  }

}
