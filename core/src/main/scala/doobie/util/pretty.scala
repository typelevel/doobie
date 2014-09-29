package doobie.util

import Predef._
import scalaz._, Scalaz._


/** Some functions for pretty-printing. */
object pretty {

  /** A block of text that can be aligned with other blocks. */
  case class Block(lines: List[String]) {
    
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
      Block(lines.map(_.padTo(width, ' ')).padTo(h, " " * width).fzipWith(other.lines.padTo(h, ""))(_ + padding + _))
    }

    def above(other: Block): Block =
      Block(lines ++ other.lines)

    def above1(other: Block): Block =
      Block(lines ++ ("" :: other.lines))

    override def toString: String =
      lines.mkString("\n")

  }

}

//  (Block(List("foo", "banana")) leftOf2 Block(List("1", "two", "seven"))).lines.foreach(println)


