// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.scalatest

import cats.effect.{ Async, IO }
import doobie.free.connection._
import doobie.util.analysis.{AlignmentError, Analysis}
import doobie.util.pretty._
import doobie.util.pos.Pos
import doobie.util.query.{Query, Query0}
import doobie.util.transactor.Transactor
import doobie.util.update.{Update, Update0}
import org.scalatest.Assertions
import scala.reflect.runtime.universe.WeakTypeTag

/**
  * Mix-in trait for specifications that enables checking of doobie `Query` and `Update` values.
  * Users must provide an effect type `M` as well as a `Transactor[M]` and instances. As a
  * convenience doobie provides specializations for common effect types (see other types in this
  * package).
  *
  * {{{
  * // An example specification, taken from the examples project.
  * class ExampleSpec extends FunSuite with IOChecker {
  *
  *   // The transactor to use for the tests.
  *   val transactor = Transactor.fromDriverManager[IO](
  *     "org.postgresql.Driver",
  *     "jdbc:postgresql:world",
  *     "postgres", ""
  *   )
  *
  *   // Now just mention the queries. Arguments are not used.
  *   test("findByNameAndAge") { check(MyDaoModule.findByNameAndAge(null, 0)) }
  *   test("allWoozles") { check(MyDaoModule.allWoozles) }
  *
  * }
  * }}}
  */
trait Checker[M[_]] {
  self: Assertions =>

  // Effect type, required instances, unsafe run
  implicit val M: Async[M]
  def unsafeRunSync[A](ma: M[A]): A

  def transactor: Transactor[M]

  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  def check[A, B](q: Query[A, B])(implicit A: WeakTypeTag[A], B: WeakTypeTag[B]) =
    checkAnalysis(s"Query[${typeName(A)}, ${typeName(B)}]", q.pos, q.sql, q.analysis)

  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  def check[A](q: Query0[A])(implicit A: WeakTypeTag[A]) =
    checkAnalysis(s"Query0[${typeName(A)}]", q.pos, q.sql, q.analysis)

  def checkOutput[A](q: Query0[A])(implicit A: WeakTypeTag[A]) =
    checkAnalysis(s"Query0[${typeName(A)}]", q.pos, q.sql, q.outputAnalysis)

  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  def check[A](q: Update[A])(implicit A: WeakTypeTag[A]) =
    checkAnalysis(s"Update[${typeName(A)}]", q.pos, q.sql, q.analysis)

  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  def check[A](q: Update0) =
    checkAnalysis(s"Update0", q.pos, q.sql, q.analysis)

  /** Check if the analysis has an error */
  private def hasError(analysisAttempt: Either[Throwable, Analysis]): Boolean =
    analysisAttempt match {
      case Left(_) => true
      case Right(a) =>
        !(a.paramDescriptions.map { case (_, es) => es.isEmpty } ++ a.columnDescriptions.map {
          case (_, es)                           => es.isEmpty
        }).forall(x => x)
    }

  private def checkAnalysis(
    typeName: String,
    pos:      Option[Pos],
    sql:      String,
    analysis: ConnectionIO[Analysis]
  ) = {
    val analysisAttempt = unsafeRunSync(M.attempt(transactor.trans(M).apply(analysis)))
    if (hasError(analysisAttempt)) {
      val analysisOutput = analysisAttempt match {
        case Left(e) =>
          failure("SQL fails typechecking", formatError(e.getMessage))
        case Right(a) =>
          success("SQL compiles and typechecks", None) +
            a.paramDescriptions.map { case (s, es) => assertEmpty(s, es) }
              .map(s => s"  $s")
              .mkString +
            a.columnDescriptions.map { case (s, es) => assertEmpty(s, es) }
              .map(s => s"  $s")
              .mkString
      }
      fail(s"  $typeName defined at ${loc(pos)}\n${formatSql(sql)}\n  ${analysisOutput}")
    }
  }

  private val packagePrefix = "\\b[a-z]+\\.".r

  private def loc(f: Option[Pos]): String =
    f.map(f => s"${f.file}:${f.line}").getOrElse("(source location unknown)")

  private def assertEmpty(name: String, es: List[AlignmentError]): String =
    if (es.isEmpty) success(name, None)
    else failure(name, es.map(formatAlignmentError).mkString("\n"))

  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  private def typeName[A](tag: WeakTypeTag[A]): String =
    packagePrefix.replaceAllIn(tag.tpe.toString, "")

  private def formatAlignmentError(e: AlignmentError): String =
    formatError(e.msg)

  private def formatError(s: String): String =
    (wrap(80)(s) match {
      case s :: ss => (s"${Console.RED}  - $s${Console.RESET}") :: ss.map(s => s"${Console.RED}    $s${Console.RESET}")
      case Nil     => Nil
    }).mkString("\n")

  private def formatSql(sql: String): String =
    sql.lines.dropWhile(_.trim.isEmpty).map(s => s"  \033[37m$s${Console.RESET}").mkString("\n", "\n", "")

  private def failure(name: String, desc: String): String =
    s"${Console.RED}  ✕ ${Console.RESET}$name\n" + desc.lines.map(s => s"  $s").mkString("", "\n", "\n")

  private def success(name: String, desc: Option[String]): String =
    s"${Console.GREEN}  ✓ ${Console.RESET}$name\n" + desc.toList.mkString("\n")
}

/** Implementation of Checker[IO] */
trait IOChecker extends Checker[IO] {
  self: Assertions =>
  val M: Async[IO] = implicitly
  def unsafeRunSync[A](ma: IO[A]) = ma.unsafeRunSync
}
