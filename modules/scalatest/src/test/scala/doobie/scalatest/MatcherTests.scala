// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.scalatest

import cats.effect.IO
import org.typelevel.doobie.syntax.string.*
import org.typelevel.doobie.util.transactor.Transactor
import org.scalatest.*

trait MatcherChecks[M[_]] extends funsuite.AnyFunSuite
    with matchers.must.Matchers
    with AnalysisMatchers[M] {

  lazy val transactor: Transactor[M] = Transactor.fromDriverManager[M](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None
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

class IOMatcherCheck extends MatcherChecks[IO] with IOChecker
