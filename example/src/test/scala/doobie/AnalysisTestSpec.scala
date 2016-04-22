package doobie.example

import doobie.imports._
import doobie.contrib.specs2.analysisspec._

import org.specs2.mutable.Specification

import scalaz.concurrent.Task

object AnalysisTestSpec extends Specification with AnalysisSpec[DriverManagerTransactor] {
  val transactor = DriverManagerTransactor("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")
  val ev: Transactor[Task, DriverManagerTransactor] = implicitly
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

