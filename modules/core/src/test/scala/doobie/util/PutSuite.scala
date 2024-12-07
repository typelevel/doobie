// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.IO
import doobie.testutils.VoidExtensions
import doobie.util.transactor.Transactor

class PutSuite extends munit.FunSuite {
  case class X(x: Int)

  case class Q(x: String)

  case class Reg1(x: Int)

  case class Reg2(x: Int)

  val xa = Transactor.fromDriverManager[IO](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None
  )

  case class Foo(s: String)

  case class Bar(n: Int)

  test("Put should exist for primitive types") {
    Put[Int].void
    Put[String].void
  }

}
