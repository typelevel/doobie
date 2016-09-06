package doobie.scalatest

import doobie.free.connection.ConnectionIO

import doobie.util.transactor._
import doobie.util.query._
import doobie.util.update._
import doobie.util.analysis._
import doobie.util.pretty._
import doobie.util.iolite.IOLite

import doobie.free.connection.ConnectionIO
import doobie.util.analysis._
import doobie.util.iolite.IOLite
import doobie.util.pretty._
import doobie.util.query._
import doobie.util.transactor._
import doobie.util.update._

import org.scalatest.{ FreeSpec, Matchers, TestSuite }

import scala.reflect.runtime.universe.TypeTag

#+scalaz
import scalaz.{ -\/, \/- }
#-scalaz
#+cats
import scala.util.{ Left => -\/, Right => \/- }
#-cats

/**
 * Module with a mix-in trait for specifications that enables checking of doobie `Query` and `Update` values.
 * {{{
 * // An example specification, taken from the examples project.
 * object AnalysisTestSpec extends AnalysisSpec {
 *
 *   // The transactor to use for the tests.
 *   val transactor = DriverManagerTransactor[IOLite](
 *     "org.postgresql.Driver", 
 *     "jdbc:postgresql:world", 
 *     "postgres", ""
 *   )
 *
 *   // Now just mention the queries. Arguments are not used.
 *   check(MyDaoModule.findByNameAndAge(null, 0))
 *   check(MyDaoModule.allWoozles)
 *
 * }
 * }}}
 */

object analysisspec {

  type IgnoreReason = String
  type ColumnName   = String

  trait AnalysisSpec extends FreeSpec { self: TestSuite =>
    def transactor: Transactor[IOLite]

    def ignoreColumns: Map[ColumnName, IgnoreReason] = Map.empty[ColumnName, IgnoreReason]

    def check[A, B](q: Query[A, B])(implicit A: TypeTag[A], B: TypeTag[B]) =
      checkAnalysis(s"Query[${typeName(A)}, ${typeName(B)}]", q.stackFrame, q.sql, q.analysis)

    def check[A](q: Query0[A])(implicit A: TypeTag[A]) =
      checkAnalysis(s"Query0[${typeName(A)}]", q.stackFrame, q.sql, q.analysis)

    def checkOutput[A](q: Query0[A])(implicit A: TypeTag[A]) =
      checkAnalysis(s"Query0[${typeName(A)}]", q.stackFrame, q.sql, q.outputAnalysis)

    def check[A](q: Update[A])(implicit A: TypeTag[A]) =
      checkAnalysis(s"Update[${typeName(A)}]", q.stackFrame, q.sql, q.analysis)

    def check[A](q: Update0)(implicit A: TypeTag[A]) =
      checkAnalysis(s"Update0", q.stackFrame, q.sql, q.analysis)

    private def checkAnalysis(typeName: String,
                              stackFrame: Option[StackTraceElement],
                              sql: String,
                              analysis: ConnectionIO[Analysis]) =
      s"$typeName defined at ${loc(stackFrame)}\n${sql.lines.map(s => "  " + s.trim).filterNot(_.isEmpty).mkString("\n")}" - {

        "SQL Compiles and Typechecks" - {
          transactor.trans(analysis).attempt.unsafePerformIO match {
            case -\/(e) =>
              "" in {
                fail(formatError(e.getMessage))
              }
            case \/-(a) =>
              a.paramDescriptions.foreach(x => assertIfNotIgnored(x._1, x._2, stackFrame))
              a.columnDescriptions.foreach(x => assertIfNotIgnored(x._1, x._2, stackFrame))
          }
        }
      }

    private def assertIfNotIgnored(s: String,
                                   es: List[AlignmentError],
                                   stackFrame: Option[StackTraceElement]) =
      (for {
        column <- extractColumn.findFirstIn(s)
        reason <- ignoreColumns.get(column)
      } yield s + s"[ignore reason: $reason]" ignore {}).getOrElse {
        assertEmpty(s, es, stackFrame)
      }

    private def loc(f: Option[StackTraceElement]): String =
      f.map(f => s"${f.getFileName}:${f.getLineNumber}").getOrElse("(source location unknown)")

    private def assertEmpty(s: String, es: List[AlignmentError], f: Option[StackTraceElement]) =
      s in {
        if (es.isEmpty) succeed
        else fail(es.map(formatError).mkString("\n"))
      }

    private val packagePrefix = "\\b[a-z]+\\.".r

    private def typeName[A](tag: TypeTag[A]): String =
      packagePrefix.replaceAllIn(tag.tpe.toString, "")

    private def formatError(e: AlignmentError): String =
      formatError(e.msg)

    private def formatError(s: String): String =
      (wrap(80)(s) match {
        case s :: ss => (s"x " + s) :: ss.map("  " + _)
        case Nil     => Nil
      }).mkString("\n")

    private val extractColumn = """(?<=\()(.*?)(?=\))""".r

  }

}
