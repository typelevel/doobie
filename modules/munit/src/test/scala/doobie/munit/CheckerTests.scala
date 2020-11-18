// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.munit

import cats.effect.{ ContextShift, IO }
import doobie.syntax.string._
import doobie.util.transactor.Transactor
import munit._
import scala.concurrent.ExecutionContext

trait CheckerChecks[M[_]] extends FunSuite with Checker[M] {

  implicit def contextShift: ContextShift[M]

  lazy val transactor = Transactor.fromDriverManager[M](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  test("trivial") { check(sql"select 1".query[Int]) }

}

class IOCheckerCheck extends CheckerChecks[IO] with IOChecker {
  def contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)
}
