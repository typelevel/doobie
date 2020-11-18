// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.munit

import cats.effect.{ Effect, IO }
import doobie.util.query.{Query, Query0}
import doobie.util.testing._
import munit.Assertions
import scala.reflect.runtime.universe.TypeTag
import munit.Location

/**
  * Mix-in trait for specifications that enables checking of doobie `Query` and `Update` values.
  * Users must provide an effect type `M` as well as a `Transactor[M]` and instances. As a
  * convenience doobie provides specializations for common effect types (see other types in this
  * package).
  *
  * {{{
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

  def check[A: Analyzable](a: A)(implicit loc: Location) = checkImpl(Analyzable.unpack(a))

  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  def checkOutput[A: TypeTag](q: Query0[A])(implicit loc: Location) =
    checkImpl(AnalysisArgs(
      s"Query0[${typeName[A]}]", q.pos, q.sql, q.outputAnalysis
    ))

  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  def checkOutput[A: TypeTag, B: TypeTag](q: Query[A, B])(implicit loc: Location) =
    checkImpl(AnalysisArgs(
      s"Query[${typeName[A]}, ${typeName[B]}]", q.pos, q.sql, q.outputAnalysis
    ))

  private def checkImpl(args: AnalysisArgs)(implicit loc: Location) = {
    val report = analyzeIO(args, transactor).unsafeRunSync()
    if (!report.succeeded) {
      fail(
        formatReport(args, report, colors)
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
