// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util
import doobie.testutils.{VoidExtensions, assertContains}
import doobie.testutils.TestClasses.{CCIntString, PlainObj, CCAnyVal}

trait GetSuitePlatform { self: munit.FunSuite =>

  test("Get can be auto derived for unary products (AnyVal)") {
    import doobie.generic.auto.*

    Get[CCAnyVal].void
  }

  test("Get can be explicitly derived for unary products (AnyVal)") {
    Get.derived[CCAnyVal].void
  }

  test("Get should not be derived for non-unary products") {
    import doobie.generic.auto.*

    assertContains(compileErrors("Get[CCIntString]"), "implicit value")
    assertContains(compileErrors("Get[(Int, Int)]"), "implicit value")
    assertContains(compileErrors("Get[PlainObj.type]"), "implicit value")
  }

}
