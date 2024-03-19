// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.types

import doobie.postgres.types.Range.Edge._
import doobie.postgres.types.Range._
import doobie.util.invariant.InvalidValue

import java.sql.{Date, Timestamp}
import java.time.{LocalDate, LocalDateTime, OffsetDateTime, ZoneOffset}

class RangeSuite extends munit.FunSuite {

  // Decoding suites
  test("decode should return correct int4range") {
    assertEquals(decode[Int]("empty"), Right(Range[Int](None, None, `empty`)))
    assertEquals(decode[Int]("[10,100)"), Right(Range(10, 100, `[_,_)`)))
    assertEquals(decode[Int]("(10,100]"), Right(Range(10, 100, `(_,_]`)))
    assertEquals(decode[Int]("(10,100)"), Right(Range(10, 100, `(_,_)`)))
    assertEquals(decode[Int]("[10,100]"), Right(Range(10, 100, `[_,_]`)))
  }

  test("decode should return correct int8range") {
    assertEquals(decode[Long]("empty"), Right(Range[Long](None, None, `empty`)))
    assertEquals(decode[Long]("[10,100)"), Right(Range(10L, 100L, `[_,_)`)))
    assertEquals(decode[Long]("(10,100]"), Right(Range(10L, 100L, `(_,_]`)))
    assertEquals(decode[Long]("(10,100)"), Right(Range(10L, 100L, `(_,_)`)))
    assertEquals(decode[Long]("[10,100]"), Right(Range(10L, 100L, `[_,_]`)))
  }

  test("decode should return correct numrange") {
    assertEquals(decode[Float]("empty"), Right(Range[Float](None, None, `empty`)))
    assertEquals(decode[Float]("[3.14,33.14)"), Right(floatRange(`[_,_)`)))
    assertEquals(decode[Float]("(3.14,33.14]"), Right(floatRange(`(_,_]`)))
    assertEquals(decode[Float]("(3.14,33.14)"), Right(floatRange(`(_,_)`)))
    assertEquals(decode[Float]("[3.14,33.14]"), Right(floatRange(`[_,_]`)))

    assertEquals(decode[Double]("empty"), Right(Range[Double](None, None, `empty`)))
    assertEquals(decode[Double]("[3.14,33.14)"), Right(doubleRange(`[_,_)`)))
    assertEquals(decode[Double]("(3.14,33.14]"), Right(doubleRange(`(_,_]`)))
    assertEquals(decode[Double]("(3.14,33.14)"), Right(doubleRange(`(_,_)`)))
    assertEquals(decode[Double]("[3.14,33.14]"), Right(doubleRange(`[_,_]`)))

    assertEquals(decode[BigDecimal]("empty"), Right(Range[BigDecimal](None, None, `empty`)))
    assertEquals(decode[BigDecimal]("[3.14,33.14)"), Right(bigDecimalRange(`[_,_)`)))
    assertEquals(decode[BigDecimal]("(3.14,33.14]"), Right(bigDecimalRange(`(_,_]`)))
    assertEquals(decode[BigDecimal]("(3.14,33.14)"), Right(bigDecimalRange(`(_,_)`)))
    assertEquals(decode[BigDecimal]("[3.14,33.14]"), Right(bigDecimalRange(`[_,_]`)))
  }

  test("decode should return correct daterange") {
    assertEquals(decode[Date]("empty"), Right(Range[Date](None, None, `empty`)))
    assertEquals(decode[Date]("[2024-03-19,2024-03-23)"), Right(dateRange(`[_,_)`)))
    assertEquals(decode[Date]("(2024-03-19,2024-03-23]"), Right(dateRange(`(_,_]`)))
    assertEquals(decode[Date]("(2024-03-19,2024-03-23)"), Right(dateRange(`(_,_)`)))
    assertEquals(decode[Date]("[2024-03-19,2024-03-23]"), Right(dateRange(`[_,_]`)))

    assertEquals(decode[LocalDate]("empty"), Right(Range[LocalDate](None, None, `empty`)))
    assertEquals(decode[LocalDate]("[2024-03-19,2024-03-23)"), Right(localDateRange(`[_,_)`)))
    assertEquals(decode[LocalDate]("(2024-03-19,2024-03-23]"), Right(localDateRange(`(_,_]`)))
    assertEquals(decode[LocalDate]("(2024-03-19,2024-03-23)"), Right(localDateRange(`(_,_)`)))
    assertEquals(decode[LocalDate]("[2024-03-19,2024-03-23]"), Right(localDateRange(`[_,_]`)))
  }

  test("decode should return correct tsrange") {
    assertEquals(decode[Timestamp]("empty"), Right(Range[Timestamp](None, None, `empty`)))
    assertEquals(decode[Timestamp]("[2024-03-19 00:00:00.0,2024-03-23 00:00:00.0)"), Right(timestampRange(`[_,_)`)))
    assertEquals(decode[Timestamp]("(2024-03-19 00:00:00.0,2024-03-23 00:00:00.0]"), Right(timestampRange(`(_,_]`)))
    assertEquals(decode[Timestamp]("(2024-03-19 00:00:00.0,2024-03-23 00:00:00.0)"), Right(timestampRange(`(_,_)`)))
    assertEquals(decode[Timestamp]("[2024-03-19 00:00:00.0,2024-03-23 00:00:00.0]"), Right(timestampRange(`[_,_]`)))

    assertEquals(decode[LocalDateTime]("empty"), Right(Range[LocalDateTime](None, None, `empty`)))
    assertEquals(decode[LocalDateTime]("[2024-03-19 00:00:00,2024-03-23 00:00:00)"), Right(localDateTimeRange(`[_,_)`)))
    assertEquals(decode[LocalDateTime]("(2024-03-19 00:00:00,2024-03-23 00:00:00]"), Right(localDateTimeRange(`(_,_]`)))
    assertEquals(decode[LocalDateTime]("(2024-03-19 00:00:00,2024-03-23 00:00:00)"), Right(localDateTimeRange(`(_,_)`)))
    assertEquals(decode[LocalDateTime]("[2024-03-19 00:00:00,2024-03-23 00:00:00]"), Right(localDateTimeRange(`[_,_]`)))
  }

  test("decode should return correct tstzrange") {
    assertEquals(decode[OffsetDateTime]("empty"), Right(Range[OffsetDateTime](None, None, `empty`)))
    assertEquals(decode[OffsetDateTime]("[2024-03-19 00:00:00+00,2024-03-23 00:00:00+00)"), Right(offsetDateTimeRange(`[_,_)`)))
    assertEquals(decode[OffsetDateTime]("(2024-03-19 00:00:00+00,2024-03-23 00:00:00+00]"), Right(offsetDateTimeRange(`(_,_]`)))
    assertEquals(decode[OffsetDateTime]("(2024-03-19 00:00:00+00,2024-03-23 00:00:00+00)"), Right(offsetDateTimeRange(`(_,_)`)))
    assertEquals(decode[OffsetDateTime]("[2024-03-19 00:00:00+00,2024-03-23 00:00:00+00]"), Right(offsetDateTimeRange(`[_,_]`)))
  }

  test("decode should not return correct range") {
    assertEquals(decode[Int]("invalid_value"), Left(InvalidValue[String, Range[Int]](value = "invalid_value", reason = "the value does not conform to the range type")))
    assertEquals(decode[Int]("[+++,+++]"), Left(InvalidValue[String, Range[Int]](value = "[+++,+++]", reason = "For input string: \"+++\"")))
    assertEquals(decode[Long]("[+++,+++]"), Left(InvalidValue[String, Range[Long]](value = "[+++,+++]", reason = "For input string: \"+++\"")))
    assertEquals(decode[Float]("[+++,+++]"), Left(InvalidValue[String, Range[Float]](value = "[+++,+++]", reason = "For input string: \"+++\"")))
    assertEquals(decode[Double]("[+++,+++]"), Left(InvalidValue[String, Range[Double]](value = "[+++,+++]", reason = "For input string: \"+++\"")))
    assertEquals(decode[Date]("[+++,+++]"), Left(InvalidValue[String, Range[Date]](value = "[+++,+++]", reason = "unknown reason")))
    assertEquals(decode[LocalDate]("[+++,+++]"), Left(InvalidValue[String, Range[LocalDate]](value = "[+++,+++]", reason = "Text '+++' could not be parsed at index 1")))
    assertEquals(decode[LocalDateTime]("[+++,+++]"), Left(InvalidValue[String, Range[LocalDateTime]](value = "[+++,+++]", reason = "Text '+++' could not be parsed at index 1")))
    assertEquals(decode[OffsetDateTime]("[+++,+++]"), Left(InvalidValue[String, Range[OffsetDateTime]](value = "[+++,+++]", reason = "Text '+++' could not be parsed at index 1")))
  }

  // Encoding suites
  test("encode should return correct int4range string value") {
    assertEquals(encode(Range[Int](None, None, `empty`)), "empty")
    assertEquals(encode(Range(10, 100, `[_,_)`)), "[10,100)")
    assertEquals(encode(Range(10, 100, `(_,_]`)), "(10,100]")
    assertEquals(encode(Range(10, 100, `(_,_)`)), "(10,100)")
    assertEquals(encode(Range(10, 100, `[_,_]`)), "[10,100]")
  }

  test("encode should return correct int8range string value") {
    assertEquals(encode(Range[Long](None, None, `empty`)), "empty")
    assertEquals(encode(Range(10L, 100L, `[_,_)`)), "[10,100)")
    assertEquals(encode(Range(10L, 100L, `(_,_]`)), "(10,100]")
    assertEquals(encode(Range(10L, 100L, `(_,_)`)), "(10,100)")
    assertEquals(encode(Range(10L, 100L, `[_,_]`)), "[10,100]")
  }

  test("encode should return correct numrange string value") {
    assertEquals(encode(Range[Double](None, None, `empty`)), "empty")
    assertEquals(encode(floatRange(`[_,_)`)), "[3.14,33.14)")
    assertEquals(encode(floatRange(`(_,_]`)), "(3.14,33.14]")
    assertEquals(encode(floatRange(`(_,_)`)), "(3.14,33.14)")
    assertEquals(encode(floatRange(`[_,_]`)), "[3.14,33.14]")
    assertEquals(encode(floatRange(`[_,_]`)), "[3.14,33.14]")

    assertEquals(encode(Range[Double](None, None, `empty`)), "empty")
    assertEquals(encode(doubleRange(`[_,_)`)), "[3.14,33.14)")
    assertEquals(encode(doubleRange(`(_,_]`)), "(3.14,33.14]")
    assertEquals(encode(doubleRange(`(_,_)`)), "(3.14,33.14)")
    assertEquals(encode(doubleRange(`[_,_]`)), "[3.14,33.14]")
    assertEquals(encode(doubleRange(`[_,_]`)), "[3.14,33.14]")

    assertEquals(encode(Range[BigDecimal](None, None, `empty`)), "empty")
    assertEquals(encode(bigDecimalRange(`[_,_)`)), "[3.14,33.14)")
    assertEquals(encode(bigDecimalRange(`(_,_]`)), "(3.14,33.14]")
    assertEquals(encode(bigDecimalRange(`(_,_)`)), "(3.14,33.14)")
    assertEquals(encode(bigDecimalRange(`[_,_]`)), "[3.14,33.14]")
    assertEquals(encode(bigDecimalRange(`[_,_]`)), "[3.14,33.14]")
  }

  test("encode should return correct daterange string value") {
    assertEquals(encode(dateRange(`[_,_)`)), "[2024-03-19,2024-03-23)")
    assertEquals(encode(dateRange(`(_,_]`)), "(2024-03-19,2024-03-23]")
    assertEquals(encode(dateRange(`(_,_)`)), "(2024-03-19,2024-03-23)")
    assertEquals(encode(dateRange(`[_,_]`)), "[2024-03-19,2024-03-23]")

    assertEquals(encode(localDateRange(`[_,_)`)), "[2024-03-19,2024-03-23)")
    assertEquals(encode(localDateRange(`(_,_]`)), "(2024-03-19,2024-03-23]")
    assertEquals(encode(localDateRange(`(_,_)`)), "(2024-03-19,2024-03-23)")
    assertEquals(encode(localDateRange(`[_,_]`)), "[2024-03-19,2024-03-23]")
  }

  test("encode should return correct tsrange string value") {
    assertEquals(encode(timestampRange(`[_,_)`)), "[2024-03-19 00:00:00.0,2024-03-23 00:00:00.0)")
    assertEquals(encode(timestampRange(`(_,_]`)), "(2024-03-19 00:00:00.0,2024-03-23 00:00:00.0]")
    assertEquals(encode(timestampRange(`(_,_)`)), "(2024-03-19 00:00:00.0,2024-03-23 00:00:00.0)")
    assertEquals(encode(timestampRange(`[_,_]`)), "[2024-03-19 00:00:00.0,2024-03-23 00:00:00.0]")

    assertEquals(encode(localDateTimeRange(`[_,_)`)), "[2024-03-19 00:00:00,2024-03-23 00:00:00)")
    assertEquals(encode(localDateTimeRange(`(_,_]`)), "(2024-03-19 00:00:00,2024-03-23 00:00:00]")
    assertEquals(encode(localDateTimeRange(`(_,_)`)), "(2024-03-19 00:00:00,2024-03-23 00:00:00)")
    assertEquals(encode(localDateTimeRange(`[_,_]`)), "[2024-03-19 00:00:00,2024-03-23 00:00:00]")
  }

  test("encode should return correct tstzrange string value") {
    assertEquals(encode(offsetDateTimeRange(`[_,_)`)), "[2024-03-19 00:00:00+00,2024-03-23 00:00:00+00)")
    assertEquals(encode(offsetDateTimeRange(`(_,_]`)), "(2024-03-19 00:00:00+00,2024-03-23 00:00:00+00]")
    assertEquals(encode(offsetDateTimeRange(`(_,_)`)), "(2024-03-19 00:00:00+00,2024-03-23 00:00:00+00)")
    assertEquals(encode(offsetDateTimeRange(`[_,_]`)), "[2024-03-19 00:00:00+00,2024-03-23 00:00:00+00]")
  }

  private def floatRange(edge: Edge): Range[Float] = Range[Float](Some(3.14F), Some(33.14F), edge)

  private def doubleRange(edge: Edge): Range[Double] = Range[Double](Some(3.14), Some(33.14), edge)

  private def bigDecimalRange(edge: Edge): Range[BigDecimal] = Range[BigDecimal](Some(BigDecimal(3.14)), Some(BigDecimal(33.14)), edge)

  private def dateRange(edge: Edge): Range[Date] = {
    val localRange = localDateRange(edge)

    Range(localRange.lowerBound.map(Date.valueOf), localRange.upperBound.map(Date.valueOf), edge)
  }

  private def timestampRange(edge: Edge): Range[Timestamp] =
    Range(
      lowerBound = Some(Timestamp.valueOf(LocalDateTime.of(2024, 3, 19, 0, 0))),
      upperBound = Some(Timestamp.valueOf(LocalDateTime.of(2024, 3, 23, 0, 0))),
      edge = edge
    )

  private def localDateRange(edge: Edge): Range[LocalDate] =
    Range(Some(LocalDate.of(2024, 3, 19)), Some(LocalDate.of(2024, 3, 23)), edge)

  private def localDateTimeRange(edge: Edge): Range[LocalDateTime] =
    Range(Some(LocalDateTime.of(2024, 3, 19, 0, 0)), Some(LocalDateTime.of(2024, 3, 23, 0, 0)), edge)

  private def offsetDateTimeRange(edge: Edge): Range[OffsetDateTime] =
    Range(
      lowerBound = Some(OffsetDateTime.of(LocalDateTime.of(2024, 3, 19, 0, 0), ZoneOffset.UTC)),
      upperBound = Some(OffsetDateTime.of(LocalDateTime.of(2024, 3, 23, 0, 0), ZoneOffset.UTC)),
      edge = edge
    )
}
