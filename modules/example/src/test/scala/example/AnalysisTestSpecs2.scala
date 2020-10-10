// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package example

import cats.effect.{ Async, IO }
import cats.effect.unsafe.UnsafeRun
import doobie._
import doobie.specs2.analysisspec._
import org.specs2.mutable.Specification


class AnalysisTestSpecs2 extends Specification with IOChecker {

  implicit val M: Async[IO] = implicitly
  implicit val U: UnsafeRun[IO] = implicitly

  val transactor = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
  )
  // Commented tests fail!
  // check(AnalysisTest.speakerQuery(null, 0))
  check(AnalysisTest.speakerQuery2)
  check(AnalysisTest.arrayTest)
  // check(AnalysisTest.arrayTest2)
  check(AnalysisTest.pointTest)
  // check(AnalysisTest.pointTest2)
  // check(AnalysisTest.update("foo", 42))
  check(AnalysisTest.update2)
}
