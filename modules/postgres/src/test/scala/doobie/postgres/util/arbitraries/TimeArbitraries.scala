// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.util.arbitraries

import java.time._

import scala.math.Ordering.Implicits._

import org.scalacheck.Arbitrary
import org.scalacheck.Gen

// https://www.postgresql.org/docs/10/datatype-datetime.html
object TimeArbitraries {

  val MinDate = LocalDate.of(-4712, 12, 31)
  val MaxDate = LocalDate.of(5874897, 12, 31)

  val MinTimestampDate = LocalDate.of(-4712, 12, 31)
  val MaxTimestampDate = LocalDate.of(294276, 12, 30) // use 30 days to avoid needing to care about offsets

  // resolution is 1 microsecond
  private def micros(nanos: Int) = Math.floorDiv(nanos, 1000)
  private def micros(nanos: Long) = Math.floorDiv(nanos, 1000)

  // Java min/max is 18, Postgres is 15:59
  private val MaxOffsetSeconds = 16 * 60 * 60 - 1
  val MinOffset = ZoneOffset.ofTotalSeconds(-MaxOffsetSeconds)
  val MaxOffset = ZoneOffset.ofTotalSeconds(MaxOffsetSeconds)

  // for Scala 2.12
  private implicit val orderingLocalDate: Ordering[LocalDate] = new Ordering[LocalDate] {
    override def compare(x: LocalDate, y: LocalDate): Int = x compareTo y
  }

  // 4713 BC to 5874897 AD
  implicit val arbitraryLocalDate: Arbitrary[LocalDate] = Arbitrary {
    chooseT(MinDate, MaxDate, LocalDate.of(0, 1, 1), LocalDate.of(1970, 1, 1))
  }

  // 00:00:00 to 24:00:00
  implicit val arbitraryLocalTime: Arbitrary[LocalTime] = Arbitrary {
    val min = micros(LocalTime.MIN.toNanoOfDay)
    val max = micros(LocalTime.MAX.toNanoOfDay)
    val noon = micros(LocalTime.NOON.toNanoOfDay)
    Gen.chooseNum(min, max, noon).map(micros => LocalTime.ofNanoOfDay(micros * 1000))
  }

  // 00:00:00+1559 to 24:00:00-1559
  implicit val arbitraryOffsetTime: Arbitrary[OffsetTime] = Arbitrary {
    val min = LocalTime.MIN.atOffset(MaxOffset)
    val max = LocalTime.MAX.atOffset(MinOffset)
    chooseT(min, max).map { time =>
      // ideally generate without nanos, but dont want to deal with negative offsets
      val nanos = time.getNano
      time.withNano(micros(nanos) * 1000)
    }
  }

  implicit val arbitraryLocalDateTime: Arbitrary[LocalDateTime] = Arbitrary {
    for {
      date <- chooseT(MinTimestampDate, MaxTimestampDate)
      time <- arbitraryLocalTime.arbitrary
    } yield LocalDateTime.of(date, time)
  }

  implicit val arbitraryInstant: Arbitrary[Instant] = Arbitrary {
    arbitraryLocalDateTime.arbitrary.map(_.toInstant(ZoneOffset.UTC))
  }

  implicit val arbitraryZoneOffset: Arbitrary[ZoneOffset] = Arbitrary {
    chooseT(MaxOffset, MinOffset, ZoneOffset.UTC)
  }

  implicit val arbitraryOffsetDateTime: Arbitrary[OffsetDateTime] = Arbitrary {
    for {
      dateTime <- arbitraryLocalDateTime.arbitrary
      offset <- arbitraryZoneOffset.arbitrary
    } yield dateTime.atOffset(offset)
  }

  /**
   * [[Gen.chooseNum]] but allows `T` to have an `Ordering` instead of `Numeric`.
   */
  private def chooseT[T](minT: T, maxT: T, specials: T*)(implicit ord: Ordering[T], c: Gen.Choose[T]): Gen[T] = {
    val basics = List(minT, maxT)
    val basicsAndSpecials = for {
      t <- specials ++ basics if t >= minT && t <= maxT
    } yield (1, Gen.const(t))
    val other = (basicsAndSpecials.length, c.choose(minT, maxT))
    val allGens = basicsAndSpecials :+ other
    Gen.frequency(allGens: _*)
  }

}
