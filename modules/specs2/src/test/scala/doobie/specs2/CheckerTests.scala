// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.specs2

import cats.effect.IO
import doobie._, doobie.implicits._
import doobie.specs2.imports._
import org.specs2.mutable.Specification

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
trait CheckerChecks[M[_]] extends Specification with Checker[M] {
  lazy val transactor = Transactor.fromDriverManager[M](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )
  check(sql"select 1".query[Int])
}

class IOCheckerCheck extends CheckerChecks[IO] with IOChecker
