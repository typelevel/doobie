package doobie.util

import doobie.imports._
#+scalaz
import scalaz._, Scalaz._
#-scalaz
#+cats
import cats._, cats.data._, cats.implicits._, cats.functor.Contravariant
#-cats
import java.util.logging.{ Logger, Level }
import scala.concurrent.duration.{ FiniteDuration => FD }
import scala.Predef.augmentString
import shapeless.HList
import shapeless.ops.hlist.ToTraversable

/** A module of types and instances for logged statements. */
object log {

  /**
   * Algebraic type of events that can be passed to a `LogHandler`, both parameterized by the
   * argument type of the SQL input parameters (this is typically an `HList`).
   * @group Events
   */
  sealed abstract class LogEvent extends Product with Serializable {

    /** The complete SQL string as seen by JDBC. */
    def sql: String

    /** The query arguments. */
    def args: List[Any]

  }

  /** @group Events */ case class Success          (sql: String, args: List[Any], exec: FD, processing: FD                    ) extends LogEvent
  /** @group Events */ case class ProcessingFailure(sql: String, args: List[Any], exec: FD, processing: FD, failure: Throwable) extends LogEvent
  /** @group Events */ case class ExecFailure      (sql: String, args: List[Any], exec: FD,                 failure: Throwable) extends LogEvent

  /**
   * A sink for `LogEvent`s with input type `A`.
   * @group Handlers
   */
  case class LogHandler(unsafeRun: LogEvent => Unit)

  /**
   * Module of instances and constructors for `LogHandler`.
   * @group Handlers
   */
  object LogHandler {

    /**
     * A do-nothing `LogHandler`.
     * @group Constructors
     */
    val nop: LogHandler =
      LogHandler(_ => ())

    /**
     * A LogHandler that writes a default format to a JDK Logger, given an `HList` argument type
     * with `ToTraversable` evidence, as is available when using the `sql` interpolator. This is
     * provided for demonstration purposes and is not intended for production use.
     * @group Constructors
     */
    @deprecated("This example LogHandler is not intended for production use. Write your own!", "0.3.1")
    val jdkLogHandler: LogHandler = {
      val jdkLogger = Logger.getLogger(getClass.getName)
      LogHandler {

        case Success(s, a, e1, e2) =>
          jdkLogger.info(s"""Successful Statement Execution:
            |
            |  ${s.lines.dropWhile(_.trim.isEmpty).mkString("\n  ")}
            |
            | arguments = [${a.mkString(", ")}]
            |   elapsed = ${e1.toMillis} ms exec + ${e2.toMillis} ms processing (${(e1 + e2).toMillis} ms total)
          """.stripMargin)

        case ProcessingFailure(s, a, e1, e2, t) =>
          jdkLogger.severe(s"""Failed Resultset Processing:
            |
            |  ${s.lines.dropWhile(_.trim.isEmpty).mkString("\n  ")}
            |
            | arguments = [${a.mkString(", ")}]
            |   elapsed = ${e1.toMillis} ms exec + ${e2.toMillis} ms processing (failed) (${(e1 + e2).toMillis} ms total)
            |   failure = ${t.getMessage}
          """.stripMargin)

        case ExecFailure(s, a, e1, t) =>
          jdkLogger.severe(s"""Failed Statement Execution:
            |
            |  ${s.lines.dropWhile(_.trim.isEmpty).mkString("\n  ")}
            |
            | arguments = [${a.mkString(", ")}]
            |   elapsed = ${e1.toMillis} ms exec (failed)
            |   failure = ${t.getMessage}
          """.stripMargin)

      }
    }


  }

}
