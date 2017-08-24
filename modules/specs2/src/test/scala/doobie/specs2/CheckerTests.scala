package doobie.specs2

import cats.effect.IO
import doobie._, doobie.implicits._
import doobie.specs2.imports._
import org.specs2.mutable.Specification

trait CheckerChecks[M[_]] extends Specification with Checker[M] {
  lazy val transactor = Transactor.fromDriverManager[M](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )
  check(sql"select 1".query[Int])
}

class IOCheckerCheck extends CheckerChecks[IO] with IOChecker
