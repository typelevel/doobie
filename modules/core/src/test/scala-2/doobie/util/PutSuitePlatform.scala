// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util
import doobie.testutils.{VoidExtensions, assertContains}

object PutSuitePlatform {
  final case class Y(x: String) extends AnyVal
  final case class P(x: Int) extends AnyVal
}

trait PutSuitePlatform { self: munit.FunSuite =>
  import PutSuitePlatform._

  test("Put can be auto derived for unary products (AnyVal)") {
    import doobie.generic.auto._

    Put[Y].void
    Put[P].void
  }

  test("Put can be explicitly derived for unary products (AnyVal)") {
    Put.derived[Y].void
    Put.derived[P].void
  }

  test("Put should not be derived for non-unary products") {
    import doobie.generic.auto._
    import doobie.testutils.TestClasses.{CCIntString, PlainObj}

    assertContains(compileErrors("Put[CCIntString]"), "implicit value")
    assertContains(compileErrors("Put[(Int, Int)]"), "implicit value")
    assertContains(compileErrors("Put[PlainObj.type]"), "implicit value")
  }

}
