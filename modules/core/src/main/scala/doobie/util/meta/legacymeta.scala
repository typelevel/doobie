// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util.meta

trait LegacyMeta {

  object legacy {

    object instant extends LegacyInstantMetaInstance

    object localdate extends LegacyLocalDateMetaInstance

  }

}

trait LegacyLocalDateMetaInstance {

  /** @group Instances */
  implicit val JavaTimeLocalDateMeta: Meta[java.time.LocalDate] =
    Meta.DateMeta.imap(_.toLocalDate)(java.sql.Date.valueOf)

}

trait LegacyInstantMetaInstance {

  /** @group Instances */
  implicit val JavaTimeInstantMeta: Meta[java.time.Instant] =
    Meta.TimestampMeta.imap(_.toInstant)(java.sql.Timestamp.from)

}
