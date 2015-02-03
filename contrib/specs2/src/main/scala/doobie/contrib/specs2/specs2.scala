package doobie.contrib.specs2

import doobie.free.connection.ConnectionIO

import doobie.util.transactor._
import doobie.util.query._
import doobie.util.update._
import doobie.util.analysis._
import doobie.util.pretty._

import org.specs2.mutable.Specification
import org.specs2.execute.Failure

import scala.reflect.runtime.universe.TypeTag

import scalaz.concurrent.Task
import scalaz._, Scalaz._

/**
 * Module with a mix-in trait for specifications that enables checking of doobie `Query` and `Update` values.
 * {{{
 * // An example specification, taken from the examples project.
 * object AnalysisTestSpec extends Specification with AnalysisSpec {
 *
 *   // The transactor to use for the tests.
 *   val transactor = DriverManagerTransactor[Task](
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

  trait AnalysisSpec { this: Specification =>

    def transactor: Transactor[Task]

    def check[A, B](q: Query[A, B])(implicit A: TypeTag[A], B: TypeTag[B]) =
      checkAnalysis(s"Query[${typeName(A)}, ${typeName(B)}]", q.stackFrame, q.sql, q.analysis)

    def check[A](q: Query0[A])(implicit A: TypeTag[A]) =
      checkAnalysis(s"Query0[${typeName(A)}]", q.stackFrame, q.sql, q.analysis)

    def check[A](q: Update[A])(implicit A: TypeTag[A]) =
      checkAnalysis(s"Update[${typeName(A)}]", q.stackFrame, q.sql, q.analysis)

    def check[A](q: Update0)(implicit A: TypeTag[A]) =
      checkAnalysis(s"Update0", q.stackFrame, q.sql, q.analysis)

    private def checkAnalysis(typeName: String, stackFrame: Option[StackTraceElement], sql: String, analysis: ConnectionIO[Analysis]) =
      s"$typeName defined at ${loc(stackFrame)}\n${sql.lines.map(s => "  " + s.trim).filterNot(_.isEmpty).mkString("\n")}" >> {
        transactor.transact(analysis).attemptRun match {
          case -\/(e) => "SQL Compiles and Typechecks" in failure(formatError(e.getMessage))
          case \/-(a) => "SQL Compiles and Typechecks" in ok
            examplesBlock { 
              a.paramDescriptions.foreach  { case (s, es) => s in assertEmpty(es, stackFrame) }
              a.columnDescriptions.foreach { case (s, es) => s in assertEmpty(es, stackFrame) }
            }
        }
      }

    private def loc(f: Option[StackTraceElement]): String = 
      f.map(f => s"${f.getFileName}:${f.getLineNumber}").getOrElse("(source location unknown)")

    private def assertEmpty(es: List[AlignmentError], f: Option[StackTraceElement]) = 
      if (es.isEmpty) success 
      else new Failure(es.map(formatError).mkString("\n"), "", f.toList)

    private val packagePrefix = "\\b[a-z]+\\.".r

    private def typeName[A](tag: TypeTag[A]): String =
      packagePrefix.replaceAllIn(tag.tpe.toString, "")

    private def formatError(e: AlignmentError): String =
      formatError(e.msg)

    private def formatError(s: String): String =
      (wrap(80)(s) match {
        case s :: ss => (s"x " + s) :: ss.map("  " + _)
        case Nil => Nil
      }).mkString("\n")

  }

}



