// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import doobie.testutils.VoidExtensions

class PutSuite extends munit.FunSuite with PutSuitePlatform {
  case class X(x: Int)

  case class Q(x: String)

  case class Reg1(x: Int)

  case class Reg2(x: Int)

  case class Foo(s: String)

  case class Bar(n: Int)

  test("Put should exist for primitive types") {
    Put[Int].void
    Put[String].void
  }
}
