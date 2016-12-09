package doobie.scalatest

import doobie.free.connection._
import doobie.util.analysis.{AlignmentError, Analysis}
import doobie.util.pretty._
import doobie.util.pos.Pos
import doobie.util.query.{Query, Query0}
import doobie.util.transactor.Transactor
import doobie.util.update.{Update, Update0}
import doobie.util.iolite.IOLite
import org.scalatest.FunSuite

import scala.reflect.runtime.universe.WeakTypeTag

#+scalaz
import scalaz.{ -\/, \/- }
#-scalaz
#+cats
import scala.util.{ Left => -\/, Right => \/- }
#-cats


/**
  * Mix-in trait for specifications that enables checking of doobie `Query` and `Update` values.
  * {{{
  * // An example specification, taken from the examples project.
  * class ExampleSpec extends FunSuite with QueryChecker {
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
trait QueryChecker {
  self: FunSuite =>

  def transactor: Transactor[IOLite]

  def check[A, B](q: Query[A, B])(implicit A: WeakTypeTag[A], B: WeakTypeTag[B]) =
    checkAnalysis(s"Query[${typeName(A)}, ${typeName(B)}]", q.pos, q.sql, q.analysis)

  def check[A](q: Query0[A])(implicit A: WeakTypeTag[A]) =
    checkAnalysis(s"Query0[${typeName(A)}]", q.pos, q.sql, q.analysis)

  def checkOutput[A](q: Query0[A])(implicit A: WeakTypeTag[A]) =
    checkAnalysis(s"Query0[${typeName(A)}]", q.pos, q.sql, q.outputAnalysis)

  def check[A](q: Update[A])(implicit A: WeakTypeTag[A]) =
    checkAnalysis(s"Update[${typeName(A)}]", q.pos, q.sql, q.analysis)

  def check[A](q: Update0)(implicit A: WeakTypeTag[A]) =
    checkAnalysis(s"Update0", q.pos, q.sql, q.analysis)

  /** Check if the analysis has an error */
  private def hasError(analysis: ConnectionIO[Analysis]): Boolean = {
    transactor.trans(analysis).attempt.unsafePerformIO match {
      case -\/(e) => true
      case \/-(a) =>
        !(a.paramDescriptions.map { case (s, es) => es.isEmpty } ++ a.columnDescriptions.map {
          case (s, es)                           => es.isEmpty
        }).forall(x => x)
    }
  }

  private def checkAnalysis(
    typeName: String,
    pos:      Option[Pos],
    sql:      String,
    analysis: ConnectionIO[Analysis]
  ) = {
    val analysisOutput = transactor.trans(analysis).attempt.unsafePerformIO match {
      case -\/(e) =>
        failure("SQL fails typechecking", formatError(e.getMessage))
      case \/-(a) =>
        success("SQL compiles and typechecks", None) +
          a.paramDescriptions.map { case (s, es) => assertEmpty(s, es) }
            .map(s => s"  $s")
            .mkString +
          a.columnDescriptions.map { case (s, es) => assertEmpty(s, es) }
            .map(s => s"  $s")
            .mkString
    }
    println(s"  $typeName defined at ${loc(pos)}\n${formatSql(sql)}\n  ${analysisOutput}")
  }

  private val packagePrefix = "\\b[a-z]+\\.".r

  private def loc(f: Option[Pos]): String =
    f.map(f => s"${f.file}:${f.line}").getOrElse("(source location unknown)")

  private def assertEmpty(name: String, es: List[AlignmentError]): String =
    if (es.isEmpty) success(name, None)
    else failure(name, es.map(formatError).mkString("\n"))

  private def typeName[A](tag: WeakTypeTag[A]): String =
    packagePrefix.replaceAllIn(tag.tpe.toString, "")

  private def formatError(e: AlignmentError): String =
    formatError(e.msg)

  private def formatError(s: String): String =
    (wrap(80)(s) match {
      case s :: ss => (s"${Console.RED}  - $s${Console.RESET}") :: ss.map(s => s"${Console.RED}    $s${Console.RESET}")
      case Nil     => Nil
    }).mkString("\n")

  private def formatSql(sql: String): String = {
    val line = sql.lines.dropWhile(_.trim.isEmpty).map(s => s"  \033[37m$s${Console.RESET}").mkString("")
    s"\n$line\n"
  }

  private def failure(name: String, desc: String): String =
    s"${Console.RED}  ✕ ${Console.RESET}$name\n" + desc.lines.map(s => s"  $s").mkString("\n")

  private def success(name: String, desc: Option[String]): String =
    s"${Console.GREEN}  ✓ ${Console.RESET}$name\n" + desc.mkString("\n")
}
