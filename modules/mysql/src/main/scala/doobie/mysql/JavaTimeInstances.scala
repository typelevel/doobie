// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.mysql

import java.time.OffsetDateTime
import java.time.ZoneOffset

import doobie.Meta
import doobie.enumerated.{JdbcType => JT}
import doobie.util.meta.MetaConstructors

trait JavaTimeInstances extends MetaConstructors {

  implicit val JavaTimeOffsetDateTimeMeta: Meta[java.time.OffsetDateTime] =
    Basic.oneObject(
      JT.Timestamp,
      List(JT.VarChar, JT.Date, JT.Time),
      classOf[java.time.OffsetDateTime]
    )

  implicit val JavaTimeInstantMeta: Meta[java.time.Instant] =
    JavaTimeOffsetDateTimeMeta.timap(_.toInstant)(OffsetDateTime.ofInstant(_, ZoneOffset.UTC))

  implicit val JavaTimeLocalDateTimeMeta: Meta[java.time.LocalDateTime] =
    Basic.oneObject(
      JT.Timestamp,
      List(JT.VarChar, JT.Date, JT.Time),
      classOf[java.time.LocalDateTime]
    )

  implicit val JavaTimeLocalDateMeta: Meta[java.time.LocalDate] =
    Basic.oneObject(
      JT.Date,
      List(JT.VarChar, JT.Time, JT.Timestamp),
      classOf[java.time.LocalDate]
    )

  implicit val JavaTimeLocalTimeMeta: Meta[java.time.LocalTime] =
    Basic.oneObject(
      JT.Time,
      List(JT.Date, JT.Timestamp),
      classOf[java.time.LocalTime]
    )

  implicit val JavaTimeOffsetTimeMeta: Meta[java.time.OffsetTime] =
    Basic.oneObject(
      JT.Timestamp,
      List(JT.Date, JT.Time),
      classOf[java.time.OffsetTime]
    )
}
