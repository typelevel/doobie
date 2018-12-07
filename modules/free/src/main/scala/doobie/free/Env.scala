// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.free

import cats.implicits._
import org.slf4j.Logger

/**
 * Interpreter environment.
 * @param jdbc The JDBC managed object (Connection, Statement, etc.)
 * @param logger A logger for tracing execution.
 */
@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
final case class Env[J](
  jdbc:    J,
  logger:  Logger,
  cid:     Option[String] = None,
  colored: Boolean = true
) {
  import Predef._

  private val colors: Array[String] = {
    import Console._
    val cs = Array(RED, GREEN, BLUE, YELLOW, MAGENTA, CYAN)
    cs ++ cs.map(BOLD + _)
  }

  private lazy val color  = colors(cid.hashCode.abs % colors.length)
  private lazy val cname  = jdbc.getClass.getSimpleName // TODO: make this more robust
  private lazy val hash   = System.identityHashCode(jdbc).toHexString.padTo(8, ' ')
  private lazy val prefix = s"$hash $cname".padTo(30, ' ').take(30)

  private def format(message: String): String = {
    val text = s"${cid.foldMap(_ + " ")}$prefix $message"
    if (colored) s"$color$text${Console.RESET}"
    else text
  }

  def unsafeTrace(s: String): Unit =
    logger.info(format(s))

}
