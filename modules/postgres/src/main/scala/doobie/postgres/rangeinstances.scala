// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import doobie.Meta
import doobie.postgres.types.Range
import doobie.postgres.types.Range._
import org.postgresql.util.PGobject

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField
import java.time.{LocalDate, LocalDateTime, OffsetDateTime}

trait RangeInstances {

  implicit val intBoundEncoder: RangeBoundEncoder[Int]               = _.toString
  implicit val longBoundEncoder: RangeBoundEncoder[Long]             = _.toString
  implicit val floatBoundEncoder: RangeBoundEncoder[Float]           = _.toString
  implicit val doubleBoundEncoder: RangeBoundEncoder[Double]         = _.toString
  implicit val bigDecimalBoundEncoder: RangeBoundEncoder[BigDecimal] = _.toString

  implicit val localDateBoundEncoder: RangeBoundEncoder[LocalDate] =
    toEndless[LocalDate](LocalDate.MAX, LocalDate.MIN, _.format(DateTimeFormatter.ISO_LOCAL_DATE))

  implicit val localDateTimeBoundEncoder: RangeBoundEncoder[LocalDateTime] =
    toEndless[LocalDateTime](LocalDateTime.MAX, LocalDateTime.MIN, _.format(date2DateTimeFormatter))

  implicit val offsetDateTimeBoundEncoder: RangeBoundEncoder[OffsetDateTime] =
    toEndless[OffsetDateTime](OffsetDateTime.MAX, OffsetDateTime.MIN, _.format(date2TzDateTimeFormatter))

  implicit val intBoundDecoder: RangeBoundDecoder[Int]               = java.lang.Integer.valueOf(_)
  implicit val longBoundDecoder: RangeBoundDecoder[Long]             = java.lang.Long.valueOf(_)
  implicit val floatBoundDecoder: RangeBoundDecoder[Float]           = java.lang.Float.valueOf(_)
  implicit val doubleBoundDecoder: RangeBoundDecoder[Double]         = java.lang.Double.valueOf(_)
  implicit val bigDecimalBoundDecoder: RangeBoundDecoder[BigDecimal] = BigDecimal(_)

  implicit val localDateBoundDecoder: RangeBoundDecoder[LocalDate] =
    fromEndless(LocalDate.MAX, LocalDate.MIN, LocalDate.parse(_, DateTimeFormatter.ISO_LOCAL_DATE))

  implicit val localDateTimeBoundDecoder: RangeBoundDecoder[LocalDateTime] =
    fromEndless(LocalDateTime.MAX, LocalDateTime.MIN, LocalDateTime.parse(_, date2DateTimeFormatter))

  implicit val offsetDateTimeBoundDecoder: RangeBoundDecoder[OffsetDateTime] =
    fromEndless(OffsetDateTime.MAX, OffsetDateTime.MIN, OffsetDateTime.parse(_, date2TzDateTimeFormatter))

  implicit val intRangeMeta: Meta[Range[Int]]                       = rangeMeta("int4range")
  implicit val longRangeMeta: Meta[Range[Long]]                     = rangeMeta("int8range")
  implicit val floatRangeMeta: Meta[Range[Float]]                   = rangeMeta("numrange")
  implicit val doubleRangeMeta: Meta[Range[Double]]                 = rangeMeta("numrange")
  implicit val bigDecimalRangeMeta: Meta[Range[BigDecimal]]         = rangeMeta("numrange")
  implicit val localDateRangeMeta: Meta[Range[LocalDate]]           = rangeMeta("daterange")
  implicit val localDateTimeRangeMeta: Meta[Range[LocalDateTime]]   = rangeMeta("tsrange")
  implicit val offsetDateTimeRangeMeta: Meta[Range[OffsetDateTime]] = rangeMeta("tstzrange")

  def rangeMeta[T](sqlRangeType: String)(implicit D: RangeBoundDecoder[T], E: RangeBoundEncoder[T]): Meta[Range[T]] =
    Meta.Advanced.other[PGobject](sqlRangeType).timap[Range[T]](
      o => Range.decode[T](o.getValue).toOption.orNull)(
      a => Option(a).map { a =>
        val o = new PGobject
        o.setType(sqlRangeType)
        o.setValue(Range.encode[T](a))
        o
      }.orNull
    )

  private val date2DateTimeFormatter =
    new DateTimeFormatterBuilder()
      .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
      .optionalStart()
      .appendFraction(ChronoField.NANO_OF_SECOND, 0, 6, true)
      .optionalEnd()
      .toFormatter()


  private val date2TzDateTimeFormatter =
    new DateTimeFormatterBuilder()
      .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
      .optionalStart()
      .appendFraction(ChronoField.NANO_OF_SECOND, 0, 6, true)
      .optionalEnd()
      .appendOffset("+HH:mm", "+00")
      .toFormatter()

  private def toEndless[T](max: T, min: T, encode: RangeBoundEncoder[T]): RangeBoundEncoder[T] = {
    case `max`  => "infinity"
    case `min`  => "-infinity"
    case finite => encode(finite)
  }

  private def fromEndless[T](max: T, min: T, decode: RangeBoundDecoder[T]): RangeBoundDecoder[T] = {
    case "infinity"  => max
    case "-infinity" => min
    case finite      => decode(finite)
  }
}
