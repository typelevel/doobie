// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.munit

import cats.effect.{ Effect, IO }
import doobie.util.query.{Query, Query0}
import doobie.util.testing._
import org.tpolecat.typename._
import munit.Assertions
import munit.Location

/**
 * Module with a mix-in trait for specifications that enables checking of doobie `Query` and `Update` values.
 * 
 * {{{
 * class ExampleSuite extends FunSuite with IOChecker {
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
object analysisspec {

  trait Checker[M[_]] extends CheckerBase[M] { this: Assertions =>

    def check[A: Analyzable](a: A)(implicit loc: Location) = checkImpl(Analyzable.unpack(a))

    def checkOutput[A: TypeName](q: Query0[A])(implicit loc: Location) =
      checkImpl(AnalysisArgs(
        s"Query0[${typeName[A]}]", q.pos, q.sql, q.outputAnalysis
      ))

    def checkOutput[A: TypeName, B: TypeName](q: Query[A, B])(implicit loc: Location) =
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
}
