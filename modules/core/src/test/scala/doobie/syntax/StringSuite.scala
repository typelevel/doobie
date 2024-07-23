// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.syntax

import doobie.implicits.*

class StringSuite extends munit.FunSuite {

  test("sql interpolator should support no-param queries") {
    val q = sql"foo bar baz".query[Int]
    assertEquals(q.sql, "foo bar baz")
  }

  test("sql interpolator should support atomic types") {
    val a = 1
    val b = "two"
    val q = sql"foo $a bar $b baz".query[Int]
    assertEquals(q.sql, "foo ? bar ? baz")
  }

  test("sql interpolator should handle leading params") {
    val a = 1
    val q = sql"$a bar baz".query[Int]
    assertEquals(q.sql, "? bar baz")
  }

  test("sql interpolator should support trailing params") {
    val b = "two"
    val q = sql"foo bar $b".query[Int]
    assertEquals(q.sql, "foo bar ?")
  }

  test("sql interpolator should support product params") {
    val a = (1, "two")
    val q = sql"foo bar $a".query[Int]
    assertEquals(q.sql, "foo bar ?,?")
  }

}
