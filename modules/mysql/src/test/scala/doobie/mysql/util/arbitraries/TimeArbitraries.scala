// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.mysql.util.arbitraries

import java.time._

import doobie.util.arbitraries.GenHelpers
import org.scalacheck.Arbitrary
import org.scalacheck.Gen

// https://dev.mysql.com/doc/refman/5.7/en/datetime.html
object TimeArbitraries {

  // plus and minus 2 days to avoid dealing with offsets
  val MinDate = LocalDate.of(1000, 1, 3)
  val MaxDate = LocalDate.of(9999, 12, 29)

  // max resolution is 1 microsecond
  private def micros(nanos: Long) = Math.floorDiv(nanos, 1000)

  // for Scala 2.12
  private implicit val orderingLocalDate: Ordering[LocalDate] = new Ordering[LocalDate] {
    override def compare(x: LocalDate, y: LocalDate): Int = x compareTo y
  }

  implicit val arbitraryLocalDate: Arbitrary[LocalDate] = Arbitrary {
    GenHelpers.chooseT(MinDate, MaxDate, LocalDate.of(1970, 1, 1))
  }

  // 00:00:00.000000 to 23:59:59.999999
  implicit val arbitraryLocalTime: Arbitrary[LocalTime] = Arbitrary {
    val min = micros(LocalTime.MIN.toNanoOfDay)
    val max = micros(LocalTime.MAX.toNanoOfDay)
    val noon = micros(LocalTime.NOON.toNanoOfDay)
    Gen.chooseNum(min, max, noon).map(micros => LocalTime.ofNanoOfDay(micros * 1000))
  }

  // '1000-01-01 00:00:00.000000' to '9999-12-31 23:59:59.999999'
  implicit val arbitraryLocalDateTime: Arbitrary[LocalDateTime] = Arbitrary {
    for {
      date <- GenHelpers.chooseT(MinDate, MaxDate)
      time <- arbitraryLocalTime.arbitrary
    } yield LocalDateTime.of(date, time)
  }

  // '1970-01-01 00:00:01.000000' to '2038-01-19 03:14:07.999999
  val arbitraryLocalDateTimeTimestamp: Arbitrary[LocalDateTime] = Arbitrary {
    val min = LocalDate.of(1970, 1, 2) // avoid not starting at 0 seconds on the 1st
    val max = LocalDate.of(2038, 1, 18) // avoid ending at 3am on the 19th

    for {
      date <- GenHelpers.chooseT(min, max)
      time <- arbitraryLocalTime.arbitrary
    } yield LocalDateTime.of(date, time)
  }


  implicit val arbitraryInstant: Arbitrary[Instant] = Arbitrary {
    arbitraryLocalDateTime.arbitrary.map(_.toInstant(ZoneOffset.UTC))
  }

  implicit val arbitraryOffsetDateTime: Arbitrary[OffsetDateTime] = Arbitrary {
    for {
      dateTime <- arbitraryLocalDateTime.arbitrary
      offset <- Arbitrary.arbitrary[ZoneOffset]
    } yield dateTime.atOffset(offset)
  }

}
