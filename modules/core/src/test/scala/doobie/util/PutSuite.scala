// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.{ ContextShift, IO }
import doobie._
import scala.concurrent.ExecutionContext

class PutSuite extends munit.FunSuite with PutSuitePlatform {
  case class Yes1(x: Int)
  case class Yes2(x: Yes1)

  case class No1(i: Int, s: String)
  object No2
  case class Y private (x: String)
  class No3(val x: Int)
  class No4(x: Int)

  implicit def contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  test("Put should exist for primitive types") {
    Put[Int]
    Put[String]
  }

  test("Put should be derived for unary products") {
    Put[Yes1]
    Put[Yes2]
  }

  test("Put should not be derived for non-unary products") {
    compileErrors("Put[(Int, Int)]")
    compileErrors("Put[No1]")
    compileErrors("Put[No1.type]")
    compileErrors("Put[No2.type]")
    compileErrors("Put[No3.type]")
    compileErrors("Put[No4.type]")
  }
}
