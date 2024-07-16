// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.Applicative
import cats.effect.Sync

import java.util.logging.Logger
import scala.Predef.augmentString
import scala.concurrent.duration.FiniteDuration

/** A module of types and instances for logged statements. */
object log {

  // Wrapper for a few information about a query for logging purposes
  final case class LoggingInfo(sql: String, params: Parameters, label: String)

  /** Parameters used in a query. For queries using batch arguments (e.g. updateMany) argument list is constructed
    * lazily.
    */
  sealed trait Parameters {
    def allParams: List[List[Any]] = {
      this match {
        case p: Parameters.NonBatch => List(p.paramsAsList)
        case p: Parameters.Batch    => p.paramsAsLists()
      }
    }
  }

  object Parameters {
    final case class NonBatch(paramsAsList: List[Any]) extends Parameters
    final case class Batch(paramsAsLists: () => List[List[Any]]) extends Parameters

    val nonBatchEmpty: NonBatch = NonBatch(Nil)
  }

  /** Algebraic type of events that can be passed to a `LogHandler`
    * @group Events
    */
  sealed abstract class LogEvent extends Product with Serializable {

    /** The complete SQL string as seen by JDBC. */
    def sql: String

    /** The query arguments. */
    def params: Parameters

    def label: String

  }

  /** @group Events */
  final case class Success(
      sql: String,
      params: Parameters,
      label: String,
      exec: FiniteDuration,
      processing: FiniteDuration
  ) extends LogEvent

  /** @group Events */
  final case class ProcessingFailure(
      sql: String,
      params: Parameters,
      label: String,
      exec: FiniteDuration,
      processing: FiniteDuration,
      failure: Throwable
  ) extends LogEvent

  /** @group Events */
  final case class ExecFailure(sql: String, params: Parameters, label: String, exec: FiniteDuration, failure: Throwable)
      extends LogEvent

  object LogEvent {
    def success(
        info: LoggingInfo,
        execDuration: FiniteDuration,
        processDuration: FiniteDuration
    ): Success = Success(
      sql = info.sql,
      params = info.params,
      label = info.label,
      exec = execDuration,
      processing = processDuration
    )

    def processingFailure(
        info: LoggingInfo,
        execDuration: FiniteDuration,
        processDuration: FiniteDuration,
        error: Throwable
    ): ProcessingFailure =
      ProcessingFailure(
        sql = info.sql,
        params = info.params,
        label = info.label,
        exec = execDuration,
        processing = processDuration,
        failure = error
      )

    def execFailure(
        info: LoggingInfo,
        execDuration: FiniteDuration,
        error: Throwable
    ): ExecFailure =
      ExecFailure(
        sql = info.sql,
        params = info.params,
        label = info.label,
        exec = execDuration,
        failure = error
      )
  }

  /** Provides additional processing for Doobie `LogEvent`s.
    * @group Handlers
    */
  trait LogHandler[M[_]] {
    def run(logEvent: LogEvent): M[Unit]
  }

  /** Module of instances and constructors for `LogHandler`.
    * @group Handlers
    */
  object LogHandler {
    private val jdkLogger = Logger.getLogger(getClass.getName)

    /** A LogHandler that doesn't do anything */
    def noop[M[_]: Applicative]: LogHandler[M] = {
      new LogHandler[M] {
        private val unit = Applicative[M].unit
        override def run(logEvent: LogEvent): M[Unit] = unit
      }
    }

    /** A LogHandler that writes a default format to a JDK Logger. This is provided for demonstration purposes and is
      * not intended for production use.
      * @group Constructors
      */
    def jdkLogHandler[M[_]: Sync]: LogHandler[M] = new LogHandler[M] {
      override def run(logEvent: LogEvent): M[Unit] = Sync[M].delay(
        logEvent match {
          case Success(s, a, l, e1, e2) =>
            val paramsStr = a match {
              case nonBatch: Parameters.NonBatch => s"[${nonBatch.paramsAsList.mkString(", ")}]"
              case _: Parameters.Batch           => "<batch arguments not rendered>"
            }
            jdkLogger.info(s"""Successful Statement Execution:
                              |
                              |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
                              |
                              | parameters = $paramsStr
                              | label     = $l
                              | elapsed = ${e1.toMillis.toString} ms exec + ${e2.toMillis
                               .toString} ms processing (${(e1 + e2).toMillis.toString} ms total)
              """.stripMargin)

          case ProcessingFailure(s, a, l, e1, e2, t) =>
            val paramsStr = a.allParams.map(thisArgs => thisArgs.mkString("(", ", ", ")"))
              .mkString("[", ", ", "]")
            jdkLogger.severe(s"""Failed Resultset Processing:
                                |
                                |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
                                |
                                | parameters = $paramsStr
                                | label     = $l
                                | elapsed = ${e1.toMillis.toString} ms exec + ${e2.toMillis
                                 .toString} ms processing (failed) (${(e1 + e2).toMillis.toString} ms total)
                                | failure = ${t.getMessage}
              """.stripMargin)

          case ExecFailure(s, a, l, e1, t) =>
            val paramsStr = a.allParams.map(thisArgs => thisArgs.mkString("(", ", ", ")"))
              .mkString("[", ", ", "]")
            jdkLogger.severe(s"""Failed Statement Execution:
                                |
                                |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
                                |
                                | parameters = $paramsStr
                                | label     = $l
                                | elapsed = ${e1.toMillis.toString} ms exec (failed)
                                | failure = ${t.getMessage}
              """.stripMargin)
        }
      )
    }
  }
}
