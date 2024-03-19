// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.types

import cats.Monoid
import cats.implicits.toBifunctorOps
import doobie.postgres.types.Range.Edge
import doobie.postgres.types.Range.Edge._
import doobie.util.invariant.InvalidValue

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

object Range {
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
