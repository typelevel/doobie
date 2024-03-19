// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.types

import cats.Monoid
import cats.implicits.toBifunctorOps
import doobie.postgres.types.Range.{Edge, RangeBoundDecoder, RangeBoundEncoder}
import doobie.postgres.types.Range.Edge._
import doobie.util.invariant.InvalidValue

import java.sql.{Date, Timestamp}
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField
import java.time.{LocalDate, LocalDateTime, OffsetDateTime}
import scala.util.Try

/*
  Range Input/Output
  The input for a range value must follow one of the following patterns:

  (lower-bound,upper-bound)
  (lower-bound,upper-bound]
  [lower-bound,upper-bound)
  [lower-bound,upper-bound]
  empty
*/
case class Range[T](lowerBound: Option[T], upperBound: Option[T], edge: Edge)

object Range extends RangeInstances {
  sealed trait Edge

  object Edge {
    case object `(_,_)` extends Edge
    case object `(_,_]` extends Edge
    case object `[_,_)` extends Edge
    case object `[_,_]` extends Edge
    case object `empty` extends Edge
  }

  type RangeBoundDecoder[T] = String => T
  type RangeBoundEncoder[T] = T => String

  def apply[T](start: T, end: T, edge: Edge = `[_,_)`): Range[T] = Range(Some(start), Some(end), edge)

  def encode[T](range: Range[T])(implicit E: RangeBoundEncoder[T]): String = {
    val conv: Option[T] => String = o =>
      o.map(E).getOrElse(Monoid[String].empty)

    range.edge match {
      case `empty` => "empty"
      case `[_,_)` => s"[${conv(range.lowerBound)},${conv(range.upperBound)})"
      case `(_,_]` => s"(${conv(range.lowerBound)},${conv(range.upperBound)}]"
      case `(_,_)` => s"(${conv(range.lowerBound)},${conv(range.upperBound)})"
      case `[_,_]` => s"[${conv(range.lowerBound)},${conv(range.upperBound)}]"
    }
  }

  def decode[T](range: String)(implicit D: RangeBoundDecoder[T]): Either[InvalidValue[String, Range[T]], Range[T]] = {
    def conv(start: String, end: String, edge: Edge): Either[InvalidValue[String, Range[T]], Range[T]] = {
      val conv: String => Either[InvalidValue[String, Range[T]], Option[T]] = s =>
        Try(Option(s).filter(_.nonEmpty).map(D))
          .toEither
          .leftMap { error => InvalidValue(value = range, reason = Option(error.getMessage).getOrElse("unknown reason")) }

      for {
        start <- conv(start)
        end   <- conv(end)
      } yield Range[T](start, end, edge)
    }

    range match {
      case `[_,_)Range`(start, end) => conv(start, end, `[_,_)`)
      case `(_,_]Range`(start, end) => conv(start, end, `(_,_]`)
      case `(_,_)Range`(start, end) => conv(start, end, `(_,_)`)
      case `[_,_]Range`(start, end) => conv(start, end, `[_,_]`)
      case "empty"                  => Right(Range[T](None, None, `empty`))
      case _                        => Left(InvalidValue(value = range, reason = "the value does not conform to the range type"))
    }
  }

  private val `[_,_)Range` = """\["?([^,"]*)"?,[ ]*"?([^,"]*)"?\)""".r // [_,_)
  private val `(_,_]Range` = """\("?([^,"]*)"?,[ ]*"?([^,"]*)"?\]""".r // (_,_]
  private val `(_,_)Range` = """\("?([^,"]*)"?,[ ]*"?([^,"]*)"?\)""".r // (_,_)
  private val `[_,_]Range` = """\["?([^,"]*)"?,[ ]*"?([^,"]*)"?\]""".r // [_,_]
}

sealed trait RangeInstances {

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

  implicit val IntRangeBoundEncoder: RangeBoundEncoder[Int]                       = _.toString
  implicit val LongRangeBoundEncoder: RangeBoundEncoder[Long]                     = _.toString
  implicit val FloatRangeBoundEncoder: RangeBoundEncoder[Float]                   = _.toString
  implicit val DoubleRangeBoundEncoder: RangeBoundEncoder[Double]                 = _.toString
  implicit val BigDecimalRangeBoundEncoder: RangeBoundEncoder[BigDecimal]         = _.toString
  implicit val DateRangeBoundEncoder: RangeBoundEncoder[Date]                     = _.toString
  implicit val TimestampRangeBoundEncoder: RangeBoundEncoder[Timestamp]           = _.toString
  implicit val LocalDateRangeBoundEncoder: RangeBoundEncoder[LocalDate]           = _.format(DateTimeFormatter.ISO_LOCAL_DATE)
  implicit val LocalDateTimeRangeBoundEncoder: RangeBoundEncoder[LocalDateTime]   = _.format(date2DateTimeFormatter)
  implicit val OffsetDateTimeRangeBoundEncoder: RangeBoundEncoder[OffsetDateTime] = _.format(date2TzDateTimeFormatter)

  implicit val IntRangeBoundDecoder: RangeBoundDecoder[Int]               = _.toInt
  implicit val LongRangeBoundDecoder: RangeBoundDecoder[Long]             = _.toLong
  implicit val FloatRangeBoundDecoder: RangeBoundDecoder[Float]           = _.toFloat
  implicit val DoubleRangeBoundDecoder: RangeBoundDecoder[Double]         = _.toDouble
  implicit val BigDecimalRangeBoundDecoder: RangeBoundDecoder[BigDecimal] = BigDecimal(_)
  implicit val DateRangeBoundDecoder: RangeBoundDecoder[Date]             = Date.valueOf
  implicit val TimestampRangeBoundDecoder: RangeBoundDecoder[Timestamp]   = Timestamp.valueOf

  implicit val LocalDateRangeBoundDecoder: RangeBoundDecoder[LocalDate]   =
    fromEndless(LocalDate.MAX, LocalDate.MIN, LocalDate.parse(_, DateTimeFormatter.ISO_LOCAL_DATE))

  implicit val LocalDateTimeRangeBoundDecoder: RangeBoundDecoder[LocalDateTime] =
    fromEndless(LocalDateTime.MAX, LocalDateTime.MIN, LocalDateTime.parse(_, date2DateTimeFormatter))

  implicit val OffsetDateTimeRangeBoundDecoder: RangeBoundDecoder[OffsetDateTime] =
    fromEndless(OffsetDateTime.MAX, OffsetDateTime.MIN, OffsetDateTime.parse(_, date2TzDateTimeFormatter))

  private def fromEndless[T](max: T, min: T, decode: RangeBoundDecoder[T]): RangeBoundDecoder[T] = {
    case "infinity" => max
    case "-infinity" => min
    case finite => decode(finite)
  }
}