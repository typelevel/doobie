// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util
import doobie.testutils.{VoidExtensions, assertContains}
import doobie.testutils.TestClasses.{CCIntString, PlainObj, CCAnyVal}

trait PutSuitePlatform { self: munit.FunSuite =>
  test("Put can be auto derived for unary products (AnyVal)") {
    import doobie.generic.auto.*

    Put[CCAnyVal].void
  }

  test("Put can be explicitly derived for unary products (AnyVal)") {
    Put.derived[CCAnyVal].void
  }

  test("Put should not be derived for non-unary products") {
    import doobie.generic.auto.*

    assertContains(compileErrors("Put[CCIntString]"), "implicit value")
    assertContains(compileErrors("Put[(Int, Int)]"), "implicit value")
    assertContains(compileErrors("Put[PlainObj.type]"), "implicit value")
  }

}
