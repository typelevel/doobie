package doobie.specs2

import doobie.imports._
import doobie.specs2.imports._
import org.specs2.mutable.Specification

#+cats
import fs2.Task
#-cats
#+scalaz
import scalaz.concurrent.Task
import scalaz.effect.IO
#-scalaz

trait CheckerChecks[M[_]] extends Specification with Checker[M] {
  lazy val transactor = DriverManagerTransactor[M](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )
  check(sql"select 1".query[Int])
}

class IOLiteCheckerCheck extends CheckerChecks[IOLite] with IOLiteChecker
#+cats
class TaskCheckerCheck   extends CheckerChecks[Task]   with TaskChecker
#-cats
#+scalaz
class TaskCheckerCheck   extends CheckerChecks[Task]   with TaskChecker
class IOCheckerCheck     extends CheckerChecks[IO]     with IOChecker
#-scalaz
