// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.IO
import doobie._

import scala.annotation.nowarn

class PutSuite extends munit.FunSuite with PutSuitePlatform {
  case class X(x: Int)
  case class Q(x: String)

  case class Z(i: Int, s: String)
  object S

  case class Reg1(x: Int)
  case class Reg2(x: Int)

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  case class Foo(s: String)
  case class Bar(n: Int)

  test("Put should exist for primitive types") {
    Put[Int]
    Put[String]
  }

  test("Put should be derived for unary products") {
    Put[X]
    Put[Q]
  }

  test("Put should not be derived for non-unary products") {
    compileErrors("Put[Z]")
    compileErrors("Put[(Int, Int)]")
    compileErrors("Put[S.type]")
  }: @nowarn("msg=.*pure expression does nothing in statement position.*")

}
