package doobie.specs2

import doobie.imports._
import doobie.specs2.imports._
import org.specs2.mutable.Specification

import scalaz.concurrent.Task
import scalaz.effect.IO

trait CheckerChecks[M[_]] extends Specification with Checker[M] {
  lazy val transactor = DriverManagerTransactor[M](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )
  check(sql"select 1".query[Int])
}

class IOLiteCheckerCheck extends CheckerChecks[IOLite] with IOLiteChecker
class TaskCheckerCheck   extends CheckerChecks[Task]   with TaskChecker
class IOCheckerCheck     extends CheckerChecks[IO]     with IOChecker
