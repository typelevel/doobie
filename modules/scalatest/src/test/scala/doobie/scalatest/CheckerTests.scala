// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.scalatest

import cats.effect.{ Async, IO }
import cats.effect.unsafe.UnsafeRun
import doobie.syntax.string._
import doobie.util.transactor.Transactor
import org.scalatest._

trait CheckerChecks[M[_]] extends funsuite.AnyFunSuite with matchers.should.Matchers with Checker[M] {

  lazy val transactor = Transactor.fromDriverManager[M](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  test("trivial") { check(sql"select 1".query[Int]) }

}

class IOCheckerCheck extends CheckerChecks[IO] with IOChecker {

  import cats.effect.unsafe.implicits.global
  override implicit val M: Async[IO] = IO.asyncForIO
  override implicit val U: UnsafeRun[IO] = IO.unsafeRunForIO

}
