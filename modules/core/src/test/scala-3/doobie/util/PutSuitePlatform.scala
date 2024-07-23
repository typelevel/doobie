// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util
import doobie.testutils.assertContains

import scala.annotation.nowarn

trait PutSuitePlatform { self: munit.FunSuite =>

  test("Put should be derived for unary products (AnyVal)".ignore) {}

  test("Put should not be derived for non-unary products") {
    import doobie.generic.auto.*
    import doobie.testutils.TestClasses.{CCIntString, PlainObj}

    assertContains(compileErrors("Put[CCIntString]"), "No given instance")
    assertContains(compileErrors("Put[(Int, Int)]"), "No given instance")
    assertContains(compileErrors("Put[PlainObj.type]"), "No given instance")
  }: @nowarn("msg=.*unused.*")

}
