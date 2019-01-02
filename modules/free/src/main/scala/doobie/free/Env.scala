// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.free

import org.slf4j.Logger
import scala.concurrent.ExecutionContext

/**
 * Interpreter environment.
 * @param jdbc The JDBC managed object (Connection, Statement, etc.)
 * @param logger A logger for tracing execution.
 * @param blockingContext A blocking execution context for primitive operations.
 */
@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
final case class Env[J](
  jdbc:            J,
  logger:          Logger,
  blockingContext: ExecutionContext
) {
  import Predef._

  private lazy val cname  = jdbc.getClass.getSimpleName // TODO: make this more robust
  private lazy val hash   = System.identityHashCode(jdbc).toHexString //.padTo(8, ' ')
  private lazy val prefix = s"$cname<$hash>".padTo(30, ' ').take(30)

  private def format(elapsed: Long, message: String): String = {
    f"$elapsed%4d ms $prefix $message"
  }

  def unsafeTrace[A](msg: => String)(f: J => A): A =
    if (logger.isTraceEnabled) {
      val t0 = System.currentTimeMillis
      try {
        f(jdbc)
      } finally {
        val t1 = System.currentTimeMillis
        logger.trace(format(t1 - t0, msg))
      }
    } else f(jdbc)

    def map[B](f: J => B): Env[B] =
      Env(f(jdbc), logger, blockingContext)

}
