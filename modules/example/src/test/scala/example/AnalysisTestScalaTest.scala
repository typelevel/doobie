// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package example

import cats.effect.{ ContextShift, IO }
import doobie.scalatest.IOChecker
import doobie.util.transactor.Transactor
import org.scalatest._
import scala.concurrent.ExecutionContext
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

class AnalysisTestScalaCheck extends FunSuite with Matchers with IOChecker {

  implicit def contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  implicit val logger: Logger[IO] =
    Slf4jLogger.getLogger[IO]

  val transactor = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
  )

  // Commented tests fail!
//test("speakerQuery")  { check(AnalysisTest.speakerQuery(null, 0)) }
  test("speakerQuery2") { check(AnalysisTest.speakerQuery2) }
  test("arrayTest")     { check(AnalysisTest.arrayTest) }
//test("arrayTest2")    { check(AnalysisTest.arrayTest2) }
  test("pointTest")     { check(AnalysisTest.pointTest) }
//test("pointTest2")    { check(AnalysisTest.pointTest2) }
//test("update")        { check(AnalysisTest.update("foo", 42)) }
  test("update2")       { check(AnalysisTest.update2) }

}
