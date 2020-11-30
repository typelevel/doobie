// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.{ ContextShift, IO }
import doobie._
import scala.concurrent.ExecutionContext
import shapeless.test._

object PutSuite {
  final case class Y(x: String) extends AnyVal
  final case class P(x: Int) extends AnyVal
}

class PutSuite extends munit.FunSuite {
  import PutSuite._
  case class X(x: Int)
  case class Q(x: String)

  case class Z(i: Int, s: String)
  object S

  case class Reg1(x: Int)
  case class Reg2(x: Int)

  implicit def contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

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
    Put[Y]
    Put[P]
    Put[Q]
  }

  test("Put should not be derived for non-unary products") {
    illTyped("Put[Z]")
    illTyped("Put[(Int, Int)]")
    illTyped("Put[S.type]")
  }

}
