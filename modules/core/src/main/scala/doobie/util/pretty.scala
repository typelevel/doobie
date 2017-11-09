// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.annotation.tailrec
import scala.Predef._

/** Some functions for pretty-printing. */
object pretty {

  /** A block of text that can be aligned with other blocks. */
  final case class Block(lines: List[String]) {

    lazy val width  = lines.foldLeft(0)((n, s) => n max s.length)
    lazy val height = lines.length

    def leftOf(other: Block): Block =
      leftOfP(other, "")

    def leftOf1(other: Block): Block =
      leftOfP(other, " ")

    def leftOf2(other: Block): Block =
      leftOfP(other, "  ")

    def leftOfP(other: Block, padding: String): Block = {
      val h  = height max other.height
      Block((lines.map(_.padTo(width, ' ')).padTo(h, " " * width), (other.lines.padTo(h, ""))).zipped.map(_ + padding + _))
    }

    def above(other: Block): Block =
      Block(lines ++ other.lines)

    def above1(other: Block): Block =
      Block(lines ++ ("" :: other.lines))

    override def toString: String =
      lines.mkString("\n")

    def trimLeft(n: Int): Block =
      Block(lines.map(_ drop n))

  }

  def wrap(cols: Int)(s: String): List[String] = {
    @tailrec
    def go(ws: List[String], accum: List[String]): List[String] =
      ws match {
        case Nil => accum.reverse
        case w :: ws =>
          accum match {
            case Nil      => go(ws, List(w))
            case l :: ls  =>
              val l0 = l + " " + w
              if (l0.length > cols) go(ws, w :: accum)
              else go(ws, l0 :: ls)
          }
      }

    go(s.split(" +").toList, Nil)
  }


}
