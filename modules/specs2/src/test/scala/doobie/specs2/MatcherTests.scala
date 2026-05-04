// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.specs2

import cats.effect.IO
import org.typelevel.doobie.syntax.string.*
import org.typelevel.doobie.testutils.VoidExtensions
import org.typelevel.doobie.util.transactor.Transactor
import org.specs2.mutable.Specification

trait MatcherChecks[M[_]] extends Specification
    with Checker[M]
    with AnalysisMatchers[M] {

  lazy val transactor: Transactor[M] = Transactor.fromDriverManager[M](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None
  )

  ("valid query should pass" >> {
    sql"select 1".query[Int] `must` typecheck
  }).void

  ("malformed sql should fail" >> {
    sql"not a valid sql".update.must(not(typecheck))
  }).void

  ("query with mismatched type should fail" >> {
    // explicit type on `typecheck` required for Scala 3
    sql"select 'foo'".query[Int].must(not(typecheck[org.typelevel.doobie.util.query.Query0[Int]]))
  }).void
}

class IOMatcherCheck extends MatcherChecks[IO] with IOChecker
