// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.types

import cats.Monoid
import doobie.postgres.types.Range.Edge
import doobie.postgres.types.Range.Edge._

import java.sql.{Date, Timestamp}
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField
import java.time.{LocalDate, LocalDateTime, OffsetDateTime}
import scala.util.Try

case class Range[T](lowerBound: Option[T], upperBound: Option[T], edge: Edge)

object Range {

  sealed trait Edge

  object Edge {
    case object `[_,_)` extends Edge
    case object `(_,_]` extends Edge
    case object `(_,_)` extends Edge
    case object `[_,_]` extends Edge
    case object `empty` extends Edge
  }

  type ToValueConverter[T] = String => T
  type FromValueConverter[T] = T => String

  def apply[T](start: T, end: T, edge: Edge): Range[T] = Range(Some(start), Some(end), edge)

  implicit val FromIntValueConverter: FromValueConverter[Int]                       = _.toString
  implicit val FromLongValueConverter: FromValueConverter[Long]                     = _.toString
  implicit val FromFloatValueConverter: FromValueConverter[Float]                   = _.toString
  implicit val FromDoubleValueConverter: FromValueConverter[Double]                 = _.toString
  implicit val FromBigDecimalValueConverter: FromValueConverter[BigDecimal]         = _.toString
  implicit val FromDateValueConverter: FromValueConverter[Date]                     = _.toString
  implicit val FromTimestampValueConverter: FromValueConverter[Timestamp]           = _.toString
  implicit val FromLocalDateValueConverter: FromValueConverter[LocalDate]           = _.toString
  implicit val FromLocalDateTimeValueConverter: FromValueConverter[LocalDateTime]   = _.toString
  implicit val FromOffsetDateTimeValueConverter: FromValueConverter[OffsetDateTime] = _.toString

  implicit val ToIntValueConverter: ToValueConverter[Int]               = _.toInt
  implicit val ToLongValueConverter: ToValueConverter[Long]             = _.toLong
  implicit val ToFloatValueConverter: ToValueConverter[Float]           = _.toFloat
  implicit val ToDoubleValueConverter: ToValueConverter[Double]         = _.toDouble
  implicit val ToBigDecimalValueConverter: ToValueConverter[BigDecimal] = BigDecimal(_)
  implicit val ToDateValueConverter: ToValueConverter[Date]             = Date.valueOf
  implicit val ToTimestampValueConverter: ToValueConverter[Timestamp]   = Timestamp.valueOf
  implicit val ToLocalDateValueConverter: ToValueConverter[LocalDate]   = fromEndless(LocalDate.MAX, LocalDate.MIN, LocalDate.parse(_, DateTimeFormatter.ISO_LOCAL_DATE))
  implicit val ToLocalDateTimeValueConverter: ToValueConverter[LocalDateTime] = {
    val date2DateTimeFormatter =
      new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        .optionalStart()
        .appendFraction(ChronoField.NANO_OF_SECOND, 0, 6, true)
        .optionalEnd()
        .toFormatter()

    fromEndless(LocalDateTime.MAX, LocalDateTime.MIN, LocalDateTime.parse(_, date2DateTimeFormatter))
  }

  implicit val ToOffsetDateTimeValueConverter: ToValueConverter[OffsetDateTime] = {
    val date2TzDateTimeFormatter =
      new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        .optionalStart()
        .appendFraction(ChronoField.NANO_OF_SECOND, 0, 6, true)
        .optionalEnd()
        .appendOffset("+HH:mm", "+00")
        .toFormatter()

    fromEndless(OffsetDateTime.MAX, OffsetDateTime.MIN, OffsetDateTime.parse(_, date2TzDateTimeFormatter))
  }

  def stringify[T](range: Range[T])(implicit FV: FromValueConverter[T]): String = stringifyRange(range)

  def parse[T](range: String)(implicit TV: ToValueConverter[T]): Either[Throwable, Range[T]] =
    Try {
      val conv: String => Option[T] = str =>
        Option(str).filter(_.nonEmpty).map(TV)

      range match {
        case "empty"                  => Range[T](None, None, `empty`)
        case `[_,_)Range`(start, end) => Range[T](conv(start), conv(end), `[_,_)`)
        case `(_,_]Range`(start, end) => Range[T](conv(start), conv(end), `(_,_]`)
        case `(_,_)Range`(start, end) => Range[T](conv(start), conv(end), `(_,_)`)
        case `[_,_]Range`(start, end) => Range[T](conv(start), conv(end), `[_,_]`)
      }
    }.toEither

  private val `[_,_)Range` = """\["?([^,"]*)"?,[ ]*"?([^,"]*)"?\)""".r // [_,_)
  private val `(_,_]Range` = """\("?([^,"]*)"?,[ ]*"?([^,"]*)"?\]""".r // (_,_]
  private val `(_,_)Range` = """\("?([^,"]*)"?,[ ]*"?([^,"]*)"?\)""".r // (_,_)
  private val `[_,_]Range` = """\["?([^,"]*)"?,[ ]*"?([^,"]*)"?\]""".r // [_,_]

  private def stringifyRange[T](range: Range[T])(implicit FV: FromValueConverter[T]): String = {
    def conv(value: Option[T]): String = value.map(FV).getOrElse(Monoid[String].empty)

    range.edge match {
      case `empty` => "empty"
      case `[_,_)` => s"[${conv(range.lowerBound)},${conv(range.upperBound)})"
      case `(_,_]` => s"(${conv(range.lowerBound)},${conv(range.upperBound)}]"
      case `(_,_)` => s"(${conv(range.lowerBound)},${conv(range.upperBound)})"
      case `[_,_]` => s"[${conv(range.lowerBound)},${conv(range.upperBound)}]"
    }
  }

  private def fromEndless[T](max: T, min: T, convert: ToValueConverter[T]): ToValueConverter[T] = {
    case "infinity" => max
    case "-infinity" => min
    case finite => convert(finite)
  }
}