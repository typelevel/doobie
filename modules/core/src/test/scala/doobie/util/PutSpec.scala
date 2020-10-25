// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.IO
import doobie._
import org.specs2.mutable.Specification
import shapeless.test._

object PutSpec {
  final case class Y(x: String) extends AnyVal
  final case class P(x: Int) extends AnyVal
}

class PutSpec extends Specification {
  import PutSpec._
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

  "Put" should {

    "exist for primitive types" in {
      Put[Int]
      Put[String]
      true
    }

    "be derived for unary products" in {
      Put[X]
      Put[Y]
      Put[P]
      Put[Q]
      true
    }

    "not be derived for non-unary products" in {
      illTyped("Put[Z]")
      illTyped("Put[(Int, Int)]")
      illTyped("Put[S.type]")
      true
    }

    // TODO: check null-handing when writig

  }


}
