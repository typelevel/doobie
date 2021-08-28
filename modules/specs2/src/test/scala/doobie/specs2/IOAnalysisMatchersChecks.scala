// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.specs2

import scala.concurrent.ExecutionContext

import cats.effect.IO
import org.specs2.mutable.Specification

import doobie.Transactor

class IOAnalysisMatchersChecks extends Specification with IOAnalysisMatchers {

  implicit val contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  lazy val transactor = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  "IOAnalysisMatchers should not crash during initialization" >> ok
}
