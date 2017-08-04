package doobie.scalatest

import doobie.imports._
import doobie.scalatest.imports._
import org.scalatest._

import fs2.Task

trait CheckerChecks[M[_]] extends FunSuite with Matchers with Checker[M] {
  lazy val transactor = DriverManagerTransactor[M](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )
  test("trivial") { check(sql"select 1".query[Int]) }
}

class IOLiteCheckerCheck extends CheckerChecks[IOLite] with IOLiteChecker
class TaskCheckerCheck   extends CheckerChecks[Task]   with TaskChecker
