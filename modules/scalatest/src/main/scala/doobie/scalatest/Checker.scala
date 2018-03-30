// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.scalatest

import cats.effect.{ Effect, IO }
import doobie.scalatest.formatting.formatReport
import doobie.util.query.{Query, Query0}
import doobie.util.update.{Update, Update0}
import doobie.util.testing._
import org.scalatest.Assertions
import scala.reflect.runtime.universe.TypeTag

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
trait Checker[M[_]] extends CheckerBase[M] { self: Assertions =>

  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  def check[A, B](q: Query[A, B])(implicit A: TypeTag[A], B: TypeTag[B]) =
    checkImpl(Analyzable.unpack(q))

  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  def check[A](q: Query0[A])(implicit A: TypeTag[A]) =
    checkImpl(Analyzable.unpack(q))

  def checkOutput[A](q: Query0[A])(implicit A: TypeTag[A]) =
    checkImpl(AnalysisArgs(
      s"Query0[${typeName(A)}]", q.pos, q.sql, q.outputAnalysis
    ))

  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  def check[A](q: Update[A])(implicit A: TypeTag[A]) =
    checkImpl(Analyzable.unpack(q))

  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  def check(q: Update0) =
    checkImpl(Analyzable.unpack(q))

  private def checkImpl(args: AnalysisArgs) = {
    val report = analyzeIO(args, transactor).unsafeRunSync
    if (!report.succeeded) {
      fail(
        formatReport(args, report)
          .padLeft("  ")
          .toString
      )
    }
  }
}

/** Implementation of Checker[IO] */
trait IOChecker extends Checker[IO] {
  self: Assertions =>
  val M: Effect[IO] = implicitly
}
