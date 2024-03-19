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

  implicit val IntRangeBoundEncoder: RangeBoundEncoder[Int]               = _.toString
  implicit val LongRangeBoundEncoder: RangeBoundEncoder[Long]             = _.toString
  implicit val FloatRangeBoundEncoder: RangeBoundEncoder[Float]           = _.toString
  implicit val DoubleRangeBoundEncoder: RangeBoundEncoder[Double]         = _.toString
  implicit val BigDecimalRangeBoundEncoder: RangeBoundEncoder[BigDecimal] = _.toString

  implicit val LocalDateRangeBoundEncoder: RangeBoundEncoder[LocalDate] =
    toEndless[LocalDate](LocalDate.MAX, LocalDate.MIN, _.format(DateTimeFormatter.ISO_LOCAL_DATE))

  implicit val LocalDateTimeRangeBoundEncoder: RangeBoundEncoder[LocalDateTime] =
    toEndless[LocalDateTime](LocalDateTime.MAX, LocalDateTime.MIN, _.format(date2DateTimeFormatter))

  implicit val OffsetDateTimeRangeBoundEncoder: RangeBoundEncoder[OffsetDateTime] =
    toEndless[OffsetDateTime](OffsetDateTime.MAX, OffsetDateTime.MIN, _.format(date2TzDateTimeFormatter))

  implicit val IntRangeBoundDecoder: RangeBoundDecoder[Int]               = _.toInt
  implicit val LongRangeBoundDecoder: RangeBoundDecoder[Long]             = _.toLong
  implicit val FloatRangeBoundDecoder: RangeBoundDecoder[Float]           = _.toFloat
  implicit val DoubleRangeBoundDecoder: RangeBoundDecoder[Double]         = _.toDouble
  implicit val BigDecimalRangeBoundDecoder: RangeBoundDecoder[BigDecimal] = BigDecimal(_)

  implicit val LocalDateRangeBoundDecoder: RangeBoundDecoder[LocalDate] =
    fromEndless(LocalDate.MAX, LocalDate.MIN, LocalDate.parse(_, DateTimeFormatter.ISO_LOCAL_DATE))

  implicit val LocalDateTimeRangeBoundDecoder: RangeBoundDecoder[LocalDateTime] =
    fromEndless(LocalDateTime.MAX, LocalDateTime.MIN, LocalDateTime.parse(_, date2DateTimeFormatter))

  implicit val OffsetDateTimeRangeBoundDecoder: RangeBoundDecoder[OffsetDateTime] =
    fromEndless(OffsetDateTime.MAX, OffsetDateTime.MIN, OffsetDateTime.parse(_, date2TzDateTimeFormatter))

  implicit val IntRangeMeta: Meta[Range[Int]]                       = rangeMeta("int4range")
  implicit val LongRangeMeta: Meta[Range[Long]]                     = rangeMeta("int8range")
  implicit val FloatRangeMeta: Meta[Range[Float]]                   = rangeMeta("numrange")
  implicit val DoubleRangeMeta: Meta[Range[Double]]                 = rangeMeta("numrange")
  implicit val BigDecimalRangeMeta: Meta[Range[BigDecimal]]         = rangeMeta("numrange")
  implicit val LocalDateRangeMeta: Meta[Range[LocalDate]]           = rangeMeta("daterange")
  implicit val LocalDateTimeRangeMeta: Meta[Range[LocalDateTime]]   = rangeMeta("tsrange")
  implicit val OffsetDateTimeRangeMeta: Meta[Range[OffsetDateTime]] = rangeMeta("tstzrange")

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

  private def rangeMeta[T](sqlRangeType: String)(implicit D: RangeBoundDecoder[T], E: RangeBoundEncoder[T]): Meta[Range[T]] =
    Meta.Advanced.other[PGobject](sqlRangeType).timap[Range[T]](
      o => Range.decode[T](o.getValue).toOption.orNull)(
      a => Option(a).map { a =>
        val o = new PGobject
        o.setType(sqlRangeType)
        o.setValue(Range.encode[T](a))
        o
      }.orNull
    )

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
