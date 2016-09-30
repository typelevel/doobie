package doobie.util

import doobie.imports._
#+scalaz
import scalaz._, Scalaz._
#-scalaz
#+cats
import cats._, cats.data._, cats.functor.Contravariant
#-cats
import java.util.logging.{ Logger, Level }
import scala.concurrent.duration.FiniteDuration
import scala.Predef.augmentString
import shapeless.HList
import shapeless.ops.hlist.ToTraversable

/** A module of types and instances for logged statements. */
object log {
  import LogEvent._

  sealed abstract class LogEvent[A] extends Product with Serializable {

    /** The complete SQL string as seen by JDBC. */
    def sql: String

    /** The query arguments. This is usually an HList. */
    def args: A

    /** LogEvent is a covariant functor. */
    def map[B](f: A => B): LogEvent[B] =
      traverse(f: A => Id[B])

    /** LogEvent is a traversable functor. */
    def traverse[F[_]: Functor, B](f: A => F[B]): F[LogEvent[B]] =
      this match {
        case Success (s, a, m1, m2)             => f(a).map(Success(s, _, m1, m2))
        case ProcessingFailure(s, a, m1, m2, t) => f(a).map(ProcessingFailure(s, _, m1, m2, t))
        case ExecFailure(s, a, m1, t)           => f(a).map(ExecFailure(s, _, m1, t))
      }

  }
  object LogEvent {

    case class Success          [A](sql: String, args: A, msExec: FiniteDuration, msProcessing: FiniteDuration) extends LogEvent[A]
    case class ProcessingFailure[A](sql: String, args: A, msExec: FiniteDuration, msProcessing: FiniteDuration, failure: Throwable) extends LogEvent[A]
    case class ExecFailure      [A](sql: String, args: A, msExec: FiniteDuration,                     failure: Throwable) extends LogEvent[A]

    /** LogEvent is a traversable functor. */
    implicit val LogEventTraverse: Traverse[LogEvent] =
      new Traverse[LogEvent] {
        def traverseImpl[G[_]: Applicative, A, B](fa: LogEvent[A])(f: A => G[B]): G[LogEvent[B]] =
          fa.traverse(f)
      }

  }

  case class LogHandler[A](unsafeRun: LogEvent[A] => Unit) {

    /* LogHandler is a contravariant functor. */
    def contramap[B](f: B => A): LogHandler[B] =
      LogHandler(eb => unsafeRun(eb.map(f)))

  }
  object LogHandler {

    /* LogHandler is a contravariant functor. */
    implicit val LogEventContravariant: Contravariant[LogHandler] =
      new Contravariant[LogHandler] {
        def contramap[A, B](fa: LogHandler[A])(f: B => A) =
          fa.contramap(f)
      }

    /**
     * A LogHandler that writes a default format to a JDK Logger, given an `HList` argument type
     * with `ToTraversable` evidence, as is available when using the `sql` interpolator. This is
     * provided for demonstration purposes and is not intended for production use.
     */
    @deprecated("This example LogHandler is not intended for production use. Write your own!", "0.3.1")
    def jdkLogHandler[A <: HList, L](
      implicit ev: ToTraversable.Aux[A, List, L]
    ): LogHandler[A] = {
      val jdkLogger = Logger.getLogger(getClass.getName)
      LogHandler {

        case Success(s, a, e1, e2) =>
          jdkLogger.info(s"""Successful Statement Execution:
            |
            |  ${s.lines.dropWhile(_.trim.isEmpty).mkString("\n  ")}
            |
            | arguments = [${a.toList.mkString(", ")}]
            |   elapsed = ${e1.toMillis} ms exec + ${e2.toMillis} ms processing (${(e1 + e2).toMillis} ms total)
          """.stripMargin)

        case ProcessingFailure(s, a, e1, e2, t) =>
          jdkLogger.severe(s"""Failed Resultset Processing:
            |
            |  ${s.lines.dropWhile(_.trim.isEmpty).mkString("\n  ")}
            |
            | arguments = [${a.toList.mkString(", ")}]
            |   elapsed = ${e1.toMillis} ms exec + ${e2.toMillis} ms processing (failed) (${(e1 + e2).toMillis} ms total)
            |   failure = ${t.getMessage}
          """.stripMargin)

        case ExecFailure(s, a, e1, t) =>
          jdkLogger.severe(s"""Failed Statement Execution:
            |
            |  ${s.lines.dropWhile(_.trim.isEmpty).mkString("\n  ")}
            |
            | arguments = [${a.toList.mkString(", ")}]
            |   elapsed = ${e1.toMillis} ms exec (failed)
            |   failure = ${t.getMessage}
          """.stripMargin)

      }
    }


  }

}
