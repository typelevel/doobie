// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.types

import doobie.postgres.rangeimplicits._
import doobie.postgres.types.Range.Edge._
import doobie.postgres.types.Range._
import doobie.util.invariant.InvalidValue

import java.time.{LocalDate, LocalDateTime, OffsetDateTime, ZoneOffset}

class RangeSuite extends munit.FunSuite {

  // Decoding
  test("decode should return correct int4range") {
    assertEquals(decode[Int]("empty"), Right(Range.empty[Int]))
    assertEquals(decode[Int]("[10,100)"), Right(Range(10, 100, `[_,_)`)))
    assertEquals(decode[Int]("(10,100]"), Right(Range(10, 100, `(_,_]`)))
    assertEquals(decode[Int]("(10,100)"), Right(Range(10, 100, `(_,_)`)))
    assertEquals(decode[Int]("[10,100]"), Right(Range(10, 100, `[_,_]`)))
  }

  test("decode should return correct int8range") {
    assertEquals(decode[Long]("empty"), Right(Range.empty[Long]))
    assertEquals(decode[Long]("[10,100)"), Right(Range(10L, 100L, `[_,_)`)))
    assertEquals(decode[Long]("(10,100]"), Right(Range(10L, 100L, `(_,_]`)))
    assertEquals(decode[Long]("(10,100)"), Right(Range(10L, 100L, `(_,_)`)))
    assertEquals(decode[Long]("[10,100]"), Right(Range(10L, 100L, `[_,_]`)))
  }

  test("decode should return correct numrange") {
    assertEquals(decode[Float]("empty"), Right(Range.empty[Float]))
    assertEquals(decode[Float]("[3.14,33.14)"), Right(floatRange(`[_,_)`)))
    assertEquals(decode[Float]("(3.14,33.14]"), Right(floatRange(`(_,_]`)))
    assertEquals(decode[Float]("(3.14,33.14)"), Right(floatRange(`(_,_)`)))
    assertEquals(decode[Float]("[3.14,33.14]"), Right(floatRange(`[_,_]`)))

    assertEquals(decode[Double]("empty"), Right(Range.empty[Double]))
    assertEquals(decode[Double]("[3.14,33.14)"), Right(doubleRange(`[_,_)`)))
    assertEquals(decode[Double]("(3.14,33.14]"), Right(doubleRange(`(_,_]`)))
    assertEquals(decode[Double]("(3.14,33.14)"), Right(doubleRange(`(_,_)`)))
    assertEquals(decode[Double]("[3.14,33.14]"), Right(doubleRange(`[_,_]`)))

    assertEquals(decode[BigDecimal]("empty"), Right(Range.empty[BigDecimal]))
    assertEquals(decode[BigDecimal]("[3.14,33.14)"), Right(bigDecimalRange(`[_,_)`)))
    assertEquals(decode[BigDecimal]("(3.14,33.14]"), Right(bigDecimalRange(`(_,_]`)))
    assertEquals(decode[BigDecimal]("(3.14,33.14)"), Right(bigDecimalRange(`(_,_)`)))
    assertEquals(decode[BigDecimal]("[3.14,33.14]"), Right(bigDecimalRange(`[_,_]`)))
  }

  test("decode should return correct daterange") {
    assertEquals(decode[LocalDate]("empty"), Right(Range.empty[LocalDate]))
    assertEquals(decode[LocalDate]("[-infinity,infinity]"), Right(Range(LocalDate.MIN, LocalDate.MAX, `[_,_]`)))
    assertEquals(decode[LocalDate]("[2024-01-01,2024-02-02)"), Right(localDateRange(`[_,_)`)))
    assertEquals(decode[LocalDate]("(2024-01-01,2024-02-02]"), Right(localDateRange(`(_,_]`)))
    assertEquals(decode[LocalDate]("(2024-01-01,2024-02-02)"), Right(localDateRange(`(_,_)`)))
    assertEquals(decode[LocalDate]("[2024-01-01,2024-02-02]"), Right(localDateRange(`[_,_]`)))
  }

  test("decode should return correct tsrange") {
    assertEquals(decode[LocalDateTime]("empty"), Right(Range.empty[LocalDateTime]))
    assertEquals(decode[LocalDateTime]("[-infinity,infinity]"), Right(Range(LocalDateTime.MIN, LocalDateTime.MAX, `[_,_]`)))
    assertEquals(decode[LocalDateTime]("[2024-01-01 00:00:00,2024-02-02 00:00:00)"), Right(localDateTimeRange(`[_,_)`)))
    assertEquals(decode[LocalDateTime]("(2024-01-01 00:00:00,2024-02-02 00:00:00]"), Right(localDateTimeRange(`(_,_]`)))
    assertEquals(decode[LocalDateTime]("(2024-01-01 00:00:00,2024-02-02 00:00:00)"), Right(localDateTimeRange(`(_,_)`)))
    assertEquals(decode[LocalDateTime]("[2024-01-01 00:00:00,2024-02-02 00:00:00]"), Right(localDateTimeRange(`[_,_]`)))
  }

  test("decode should return correct tstzrange") {
    assertEquals(decode[OffsetDateTime]("empty"), Right(Range.empty[OffsetDateTime]))
    assertEquals(decode[OffsetDateTime]("[-infinity,infinity]"), Right(Range(OffsetDateTime.MIN, OffsetDateTime.MAX, `[_,_]`)))
    assertEquals(decode[OffsetDateTime]("[2024-01-01 00:00:00+00,2024-02-02 00:00:00+00)"), Right(offsetDateTimeRange(`[_,_)`)))
    assertEquals(decode[OffsetDateTime]("(2024-01-01 00:00:00+00,2024-02-02 00:00:00+00]"), Right(offsetDateTimeRange(`(_,_]`)))
    assertEquals(decode[OffsetDateTime]("(2024-01-01 00:00:00+00,2024-02-02 00:00:00+00)"), Right(offsetDateTimeRange(`(_,_)`)))
    assertEquals(decode[OffsetDateTime]("[2024-01-01 00:00:00+00,2024-02-02 00:00:00+00]"), Right(offsetDateTimeRange(`[_,_]`)))
  }

  test("decode should not return correct range") {
    assertEquals(decode[Int]("invalid_value"), Left(InvalidValue[String, Range[Int]](value = "invalid_value", reason = "the value does not conform to the range type")))
    assertEquals(decode[Int]("[+++,+++]"), Left(InvalidValue[String, Range[Int]](value = "[+++,+++]", reason = "For input string: \"+++\"")))
    assertEquals(decode[Long]("[+++,+++]"), Left(InvalidValue[String, Range[Long]](value = "[+++,+++]", reason = "For input string: \"+++\"")))
    assertEquals(decode[Float]("[+++,+++]"), Left(InvalidValue[String, Range[Float]](value = "[+++,+++]", reason = "For input string: \"+++\"")))
    assertEquals(decode[Double]("[+++,+++]"), Left(InvalidValue[String, Range[Double]](value = "[+++,+++]", reason = "For input string: \"+++\"")))
    assertEquals(decode[LocalDate]("[+++,+++]"), Left(InvalidValue[String, Range[LocalDate]](value = "[+++,+++]", reason = "Text '+++' could not be parsed at index 1")))
    assertEquals(decode[LocalDateTime]("[+++,+++]"), Left(InvalidValue[String, Range[LocalDateTime]](value = "[+++,+++]", reason = "Text '+++' could not be parsed at index 1")))
    assertEquals(decode[OffsetDateTime]("[+++,+++]"), Left(InvalidValue[String, Range[OffsetDateTime]](value = "[+++,+++]", reason = "Text '+++' could not be parsed at index 1")))
  }

  // Encoding
  test("encode should return correct int4range string value") {
    assertEquals(encode(Range.empty[Int]), "empty")
    assertEquals(encode(Range(10, 100, `[_,_)`)), "[10,100)")
    assertEquals(encode(Range(10, 100, `(_,_]`)), "(10,100]")
    assertEquals(encode(Range(10, 100, `(_,_)`)), "(10,100)")
    assertEquals(encode(Range(10, 100, `[_,_]`)), "[10,100]")
  }

  test("encode should return correct int8range string value") {
    assertEquals(encode(Range.empty[Long]), "empty")
    assertEquals(encode(Range(10L, 100L, `[_,_)`)), "[10,100)")
    assertEquals(encode(Range(10L, 100L, `(_,_]`)), "(10,100]")
    assertEquals(encode(Range(10L, 100L, `(_,_)`)), "(10,100)")
    assertEquals(encode(Range(10L, 100L, `[_,_]`)), "[10,100]")
  }

  test("encode should return correct numrange string value") {
    assertEquals(encode(Range.empty[Float]), "empty")
    assertEquals(encode(floatRange(`[_,_)`)), "[3.14,33.14)")
    assertEquals(encode(floatRange(`(_,_]`)), "(3.14,33.14]")
    assertEquals(encode(floatRange(`(_,_)`)), "(3.14,33.14)")
    assertEquals(encode(floatRange(`[_,_]`)), "[3.14,33.14]")
    assertEquals(encode(floatRange(`[_,_]`)), "[3.14,33.14]")

    assertEquals(encode(Range.empty[Double]), "empty")
    assertEquals(encode(doubleRange(`[_,_)`)), "[3.14,33.14)")
    assertEquals(encode(doubleRange(`(_,_]`)), "(3.14,33.14]")
    assertEquals(encode(doubleRange(`(_,_)`)), "(3.14,33.14)")
    assertEquals(encode(doubleRange(`[_,_]`)), "[3.14,33.14]")
    assertEquals(encode(doubleRange(`[_,_]`)), "[3.14,33.14]")

    assertEquals(encode(Range.empty[BigDecimal]), "empty")
    assertEquals(encode(bigDecimalRange(`[_,_)`)), "[3.14,33.14)")
    assertEquals(encode(bigDecimalRange(`(_,_]`)), "(3.14,33.14]")
    assertEquals(encode(bigDecimalRange(`(_,_)`)), "(3.14,33.14)")
    assertEquals(encode(bigDecimalRange(`[_,_]`)), "[3.14,33.14]")
    assertEquals(encode(bigDecimalRange(`[_,_]`)), "[3.14,33.14]")
  }

  test("encode should return correct daterange string value") {
    assertEquals(encode(Range.empty[LocalDate]), "empty")
    assertEquals(encode(Range[LocalDate](LocalDate.MIN, LocalDate.MAX, `[_,_]`)), "[-infinity,infinity]")
    assertEquals(encode(localDateRange(`[_,_)`)), "[2024-01-01,2024-02-02)")
    assertEquals(encode(localDateRange(`(_,_]`)), "(2024-01-01,2024-02-02]")
    assertEquals(encode(localDateRange(`(_,_)`)), "(2024-01-01,2024-02-02)")
    assertEquals(encode(localDateRange(`[_,_]`)), "[2024-01-01,2024-02-02]")
  }

  test("encode should return correct tsrange string value") {
    assertEquals(encode(Range.empty[LocalDateTime]), "empty")
    assertEquals(encode(Range(LocalDateTime.MIN, LocalDateTime.MAX, `[_,_]`)), "[-infinity,infinity]")
    assertEquals(encode(localDateTimeRange(`[_,_)`)), "[2024-01-01 00:00:00,2024-02-02 00:00:00)")
    assertEquals(encode(localDateTimeRange(`(_,_]`)), "(2024-01-01 00:00:00,2024-02-02 00:00:00]")
    assertEquals(encode(localDateTimeRange(`(_,_)`)), "(2024-01-01 00:00:00,2024-02-02 00:00:00)")
    assertEquals(encode(localDateTimeRange(`[_,_]`)), "[2024-01-01 00:00:00,2024-02-02 00:00:00]")
  }

  test("encode should return correct tstzrange string value") {
    assertEquals(encode(Range.empty[OffsetDateTime]), "empty")
    assertEquals(encode(Range(OffsetDateTime.MIN, OffsetDateTime.MAX, `[_,_]`)), "[-infinity,infinity]")
    assertEquals(encode(offsetDateTimeRange(`[_,_)`)), "[2024-01-01 00:00:00+00,2024-02-02 00:00:00+00)")
    assertEquals(encode(offsetDateTimeRange(`(_,_]`)), "(2024-01-01 00:00:00+00,2024-02-02 00:00:00+00]")
    assertEquals(encode(offsetDateTimeRange(`(_,_)`)), "(2024-01-01 00:00:00+00,2024-02-02 00:00:00+00)")
    assertEquals(encode(offsetDateTimeRange(`[_,_]`)), "[2024-01-01 00:00:00+00,2024-02-02 00:00:00+00]")
  }

  private val dateLowerBound = OffsetDateTime.of(LocalDateTime.of(2024, 1, 1, 0, 0), ZoneOffset.UTC)
  private val dateUpperBound = OffsetDateTime.of(LocalDateTime.of(2024, 2, 2, 0, 0), ZoneOffset.UTC)

  private def floatRange(edge: Edge): Range[Float] = Range[Float](Some(3.14F), Some(33.14F), edge)

  private def doubleRange(edge: Edge): Range[Double] = Range[Double](Some(3.14), Some(33.14), edge)

  private def bigDecimalRange(edge: Edge): Range[BigDecimal] = Range[BigDecimal](Some(BigDecimal(3.14)), Some(BigDecimal(33.14)), edge)

  private def localDateRange(edge: Edge): Range[LocalDate] =
    Range(Some(dateLowerBound.toLocalDate), Some(dateUpperBound.toLocalDate), edge)

  private def localDateTimeRange(edge: Edge): Range[LocalDateTime] =
    Range(Some(dateLowerBound.toLocalDateTime), Some(dateUpperBound.toLocalDateTime), edge)

  private def offsetDateTimeRange(edge: Edge): Range[OffsetDateTime] =
    Range(
      lowerBound = Some(dateLowerBound),
      upperBound = Some(dateUpperBound),
      edge = edge
    )
}
