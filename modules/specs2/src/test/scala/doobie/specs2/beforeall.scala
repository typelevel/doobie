// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.specs2

import cats.effect.{ ContextShift, IO }
import doobie.syntax.connectionio._
import doobie.syntax.string._
import doobie.util.transactor.Transactor
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAll
import scala.concurrent.ExecutionContext

// Check that AnalysisSpec plays nice with Specs2 execution flow (issue #454)

class beforeall extends Specification with IOChecker with BeforeAll {
  // Setup
  val initQ = sql"create table some_table (value varchar not null)".update

  val targetQ = sql"select value from some_table".query[String]

  implicit def contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  val transactor = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:beforeall;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  // The test itself
  check(targetQ)

  // A hook for database initialization
  def beforeAll() = {
    initQ.run
      .transact(transactor)
      .unsafeRunSync
    ()
  }
}
