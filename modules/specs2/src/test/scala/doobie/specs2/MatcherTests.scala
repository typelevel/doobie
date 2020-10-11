// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.specs2

import cats.effect.{ Async, IO }
import cats.effect.unsafe.UnsafeRun
import doobie.syntax.string._
import doobie.util.transactor.Transactor
import org.specs2.mutable.Specification

trait MatcherChecks[M[_]] extends Specification
    with Checker[M]
    with AnalysisMatchers[M] {

  lazy val transactor = Transactor.fromDriverManager[M](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  "valid query should pass" >> {
    sql"select 1".query[Int] must typecheck
  }

  "malformed sql should fail" >> {
    sql"not a valid sql".query[Int].must(not(typecheck))
  }

  "query with mismatched type should fail" >> {
    sql"select 'foo'".query[Int].must(not(typecheck))
  }
}

class IOMatcherCheck extends MatcherChecks[IO] with IOChecker {

  import cats.effect.unsafe.implicits.global
  override implicit val M: Async[IO] = IO.asyncForIO
  override implicit val U: UnsafeRun[IO] = IO.unsafeRunForIO

}
