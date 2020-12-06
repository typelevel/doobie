// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

object PutSuitePlatform {
  final case class Y(x: String) extends AnyVal
  final case class P(x: Int) extends AnyVal
}

trait PutSuitePlatform { self: munit.FunSuite =>
  import PutSuitePlatform._

  test("Put should be derived for unary products (AnyVal)") {
    Put[Y]
    Put[P]
  }

}
