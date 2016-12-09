package doobie.example

import doobie.imports._
import doobie.scalatest.QueryChecker
import org.scalatest._

class AnalysisTestScalaCheck extends FunSuite with Matchers with QueryChecker {
  val transactor = DriverManagerTransactor[IOLite]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")

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
