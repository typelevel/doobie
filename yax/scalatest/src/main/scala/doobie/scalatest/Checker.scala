package doobie.scalatest

import doobie.free.connection._
import doobie.util.analysis.{AlignmentError, Analysis}
import doobie.util.pretty._
import doobie.util.pos.Pos
import doobie.util.query.{Query, Query0}
import doobie.util.transactor.Transactor
import doobie.util.update.{Update, Update0}
import doobie.util.iolite.IOLite

import org.scalatest.Assertions

import scala.reflect.runtime.universe.WeakTypeTag

#+scalaz
import doobie.util.capture.Capture
import scalaz.{ Monad, Catchable, \/, -\/, \/- }
#-scalaz
#+cats
import scala.util.{ Either => \/, Left => -\/, Right => \/- }
import cats.Monad
import fs2.util.{ Catchable, Suspendable }
import fs2.interop.cats._
#-cats

/**
  * Mix-in trait for specifications that enables checking of doobie `Query` and `Update` values.
  * Users must provide an effect type `M` as well as a `Transactor[M]` and instances. As a
  * convenience doobie provides specializations for common effect types (see other types in this
  * package).
  *
  * {{{
  * // An example specification, taken from the examples project.
  * class ExampleSpec extends FunSuite with IOLiteChecker {
  *
  *   // The transactor to use for the tests.
  *   val transactor = DriverManagerTransactor[IOLite](
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
  implicit val monadM: Monad[M]
  implicit val catchableM: Catchable[M]
#+scalaz
  implicit val captureM: Capture[M]
#-scalaz
#+cats
  implicit val captureM: Suspendable[M]
#-cats
  def unsafePerformIO[A](ma: M[A]): A

  def transactor: Transactor[M, _]


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
  private def hasError(analysisAttempt: Throwable \/ Analysis): Boolean =
    analysisAttempt match {
      case -\/(e) => true
      case \/-(a) =>
        !(a.paramDescriptions.map { case (s, es) => es.isEmpty } ++ a.columnDescriptions.map {
          case (s, es)                           => es.isEmpty
        }).forall(x => x)
    }

  private def checkAnalysis(
    typeName: String,
    pos:      Option[Pos],
    sql:      String,
    analysis: ConnectionIO[Analysis]
  ) = {
    val analysisAttempt = unsafePerformIO(catchableM.attempt(transactor.trans(monadM).apply(analysis)))
    if (hasError(analysisAttempt)) {
      val analysisOutput = analysisAttempt match {
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
      fail(s"  $typeName defined at ${loc(pos)}\n${formatSql(sql)}\n  ${analysisOutput}")
    }
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

  private def formatSql(sql: String): String =
    sql.lines.dropWhile(_.trim.isEmpty).map(s => s"  \033[37m$s${Console.RESET}").mkString("\n", "\n", "")

  private def failure(name: String, desc: String): String =
    s"${Console.RED}  ✕ ${Console.RESET}$name\n" + desc.lines.map(s => s"  $s").mkString("", "\n", "\n")

  private def success(name: String, desc: Option[String]): String =
    s"${Console.GREEN}  ✓ ${Console.RESET}$name\n" + desc.mkString("\n")
}

/** Implementation of Checker[IOLite] */
trait IOLiteChecker extends Checker[IOLite] {
  self: Assertions =>
  val monadM: Monad[IOLite] = implicitly
  val catchableM: Catchable[IOLite] = implicitly
#+scalaz
  val captureM: Capture[IOLite] = implicitly
#-scalaz
#+cats
  val captureM: Suspendable[IOLite] = implicitly
#-cats
  def unsafePerformIO[A](ma: IOLite[A]) = ma.unsafePerformIO
}

#+cats
import fs2.Task
import scala.concurrent.duration.Duration
import scala.concurrent.Await

/** Implementation of Checker[fs2.Task] */
trait TaskChecker extends Checker[Task] {
  self: Assertions =>
  val monadM: Monad[Task] = implicitly
  val catchableM: Catchable[Task] = implicitly
  val captureM: Suspendable[Task] = implicitly
  def unsafePerformIO[A](ma: Task[A]) = ma.unsafeRun
}
#-cats

#+scalaz
import scalaz.concurrent.Task
import scalaz.effect.IO

/** Implementation of Checker[scalaz.concurrent.Task] */
trait TaskChecker extends Checker[Task] {
  self: Assertions =>
  val monadM: Monad[Task] = implicitly
  val catchableM: Catchable[Task] = implicitly
  val captureM: Capture[Task] = implicitly
  def unsafePerformIO[A](ma: Task[A]) = ma.unsafePerformSync
}

/** Implementation of Checker[scalaz.effect.IO] */
trait IOChecker extends Checker[IO] {
  self: Assertions =>
  val monadM: Monad[IO] = implicitly
  val catchableM: Catchable[IO] = implicitly
  val captureM: Capture[IO] = implicitly
  def unsafePerformIO[A](ma: IO[A]) = ma.unsafePerformIO
}
#-scalaz
