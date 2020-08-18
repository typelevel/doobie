// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.scalatest

import cats.effect._
import doobie.syntax.string._
import doobie.util.transactor.Transactor
import org.scalatest._
import scala.concurrent.ExecutionContext


trait MatcherChecks[M[_]] extends funsuite.AnyFunSuite
    with matchers.must.Matchers
    with AnalysisMatchers[M] {

  implicit def contextShift: ContextShift[M]
  implicit def concurrent: Concurrent[M]

  lazy val transactor = Transactor.fromDriverManager[M](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  test("valid query should pass") {
    sql"select 1".query[Int] must typecheck
  }

  test("malformed sql should fail") {
    sql"not a valid sql".query[Int].must(not(typecheck))
  }

  test("query with mismatched type should fail") {
    sql"select 'foo'".query[Int].must(not(typecheck))
  }
}

class IOMatcherCheck extends MatcherChecks[IO] with IOChecker {
  implicit def contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  val concurrent: Concurrent[IO] = Concurrent[IO]
}
