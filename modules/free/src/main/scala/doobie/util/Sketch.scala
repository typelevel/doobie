// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import Predef._
import java.sql.Timestamp
import java.sql.Date

object Sketch {

  /** Get a printable summary of an arbitrary value, for logging. */
  def sketch(a: Any): String =
    a match {

      // special handling for Unit and null
      case ()        => ""
      case null      => "null"

      // other JDBC types we can just toString
      case a: Byte       => a.toString
      case a: Boolean    => a.toString
      case a: Char       => s"'$a'"
      case a: Float      => a.toString
      case a: Double     => a.toString
      case a: Int        => a.toString
      case a: Long       => a.toString
      case a: BigDecimal => a.toString
      case a: BigInt     => a.toString
      case a: String     => s""""$a"""" // todo: escape
      case a: Date       => a.toString
      case a: Timestamp  => a.toString

      // special handling for Option
      case None      => "None"
      case Some(a)   => s"Some(${sketch(a)})"

      // everything else is simple classname and a hash
      case o: Object =>
        val cname  =
          try o.getClass.getSimpleName
          catch {
            // object A { class B } ... B has an invalid simpleName so we catch that case here
            case _: java.lang.InternalError => o.getClass.getName
          }
        val hash   = System.identityHashCode(o).toHexString
        s"$cname<$hash>"

    }

}