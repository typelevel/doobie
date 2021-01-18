// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util
import cats.syntax.show._

/** Module for source positions. */
object pos {

  /** A source position. */
  final case class Pos(path: String, line: Int) {

    def file: String =
      path.lastIndexOf(java.io.File.separatorChar.toString) match {
        case -1 => path
        case n  => path.substring(n + 1)
      }

    override def toString =
      show"$file:$line"

  }

  object Pos extends PosPlatform

}
