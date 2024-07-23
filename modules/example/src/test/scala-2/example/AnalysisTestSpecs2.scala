// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package example

import cats.effect.IO
import doobie.*
import doobie.specs2.analysisspec.*
import doobie.testutils.VoidExtensions
import org.specs2.mutable.Specification

class AnalysisTestSpecs2 extends Specification with IOChecker {

  val transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql:world",
    user = "postgres",
    password = "password",
    logHandler = None
  )
  // Commented tests fail!
  // check(AnalysisTest.speakerQuery(null, 0))
  check(AnalysisTest.speakerQuery2).void
  check(AnalysisTest.arrayTest).void
  check(AnalysisTest.pointTest).void
  // check(AnalysisTest.pointTest2).void
  checkOutput(AnalysisTest.update).void
  checkOutput(AnalysisTest.update0_1("foo", "bkah")).void
  check(AnalysisTest.update0_2).void
}
