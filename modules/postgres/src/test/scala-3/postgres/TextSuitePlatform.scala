// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

trait TextSuitePlatform { self: munit.FunSuite =>
  test("derives") {
    case class Foo(a: String, b: Int) derives Text
    val _ = Foo("a", 1)
  }
}
