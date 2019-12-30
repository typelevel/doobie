// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util.arbitraries

import java.time._

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

import scala.jdk.CollectionConverters._

object TimeArbitraries {

  implicit val arbitraryLocalDate: Arbitrary[LocalDate] = Arbitrary {
    for {
      d <- Gen.chooseNum(
        LocalDate.of(-500, 1, 1).toEpochDay,
        LocalDate.of(294276, 1, 1).toEpochDay
      )
    } yield LocalDate.ofEpochDay(d)
  }

  implicit val arbitraryInstant: Arbitrary[Instant] = Arbitrary {
    for {
      l <- arbitrary[LocalDateTime]
      z <- arbitrary[ZoneOffset]
    } yield l.toInstant(z)
  }

  implicit val arbitraryLocalTime: Arbitrary[LocalTime] = Arbitrary {
    for {
      n <- Gen.chooseNum(LocalTime.MIN.toNanoOfDay, LocalTime.MAX.toNanoOfDay)
    } yield LocalTime.ofNanoOfDay(n)
  }

  implicit val arbitraryZoneOffset: Arbitrary[ZoneOffset] = Arbitrary {
    for {
      s <- Gen.chooseNum(ZoneOffset.MIN.getTotalSeconds, ZoneOffset.MAX.getTotalSeconds)
    } yield ZoneOffset.ofTotalSeconds(s)
  }

  implicit val arbitraryLocalDateTime: Arbitrary[LocalDateTime] = Arbitrary {
    for {
      o <- arbitrary[ZoneOffset]
      e <- Gen.chooseNum(
        LocalDateTime.of(-500, 1, 1, 1, 1, 1).toEpochSecond(o),
        LocalDateTime.of(294276, 1, 1, 1, 1).toEpochSecond(o)
      )
      n <- Gen.chooseNum(0, 999999999)
    } yield LocalDateTime.ofEpochSecond(e, n, o)
  }

  implicit val arbitraryZoneId: Arbitrary[ZoneId] = Arbitrary {
    for {
      k <- Gen.oneOf(ZoneId.SHORT_IDS.asScala.values)
    } yield ZoneId.of(k)
  }

  implicit val arbitraryOffsetTime: Arbitrary[OffsetTime] = Arbitrary {
    for {
      i <- arbitrary[Instant]
      z <- arbitrary[ZoneId]
    } yield OffsetTime.ofInstant(i, z)
  }

  implicit val arbitraryOffsetDateTime: Arbitrary[OffsetDateTime] = Arbitrary {
    for {
      i <- arbitrary[Instant]
      z <- arbitrary[ZoneId]
    } yield OffsetDateTime.ofInstant(i, z)
  }

  implicit val arbitraryZonedDateTime: Arbitrary[ZonedDateTime] = Arbitrary {
    for {
      l <- arbitrary[LocalDateTime]
      z <- arbitrary[ZoneId]
    } yield ZonedDateTime.of(l, z)

  }


}
