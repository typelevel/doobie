package doobie.scalatest

import doobie.imports._
import doobie.scalatest.imports._
import org.scalatest._

#+cats
import fs2.Task
#-cats
#+scalaz
import scalaz.concurrent.Task
import scalaz.effect.IO
#-scalaz

trait CheckerChecks[M[_]] extends FunSuite with Matchers with Checker[M] {
  lazy val transactor = DriverManagerTransactor[M](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )
  test("trivial") { check(sql"select 1".query[Int]) }
}

class IOLiteCheckerCheck extends CheckerChecks[IOLite] with IOLiteChecker
#+cats
class TaskCheckerCheck   extends CheckerChecks[Task]   with TaskChecker
#-cats
#+scalaz
class TaskCheckerCheck   extends CheckerChecks[Task]   with TaskChecker
class IOCheckerCheck     extends CheckerChecks[IO]     with IOChecker
#-scalaz
