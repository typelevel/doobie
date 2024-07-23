// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.syntax

import cats.syntax.all.*
import doobie.implicits.*

class PutSuite extends munit.FunSuite {

  test("Put syntaxt should convert to a fragment") {
    assertEquals((fr"SELECT" ++ 1.fr).query[Unit].sql, "SELECT ? ")
  }

  test("Put syntaxt should convert to a fragment0") {
    assertEquals((fr"SELECT" ++ 1.fr0).query[Unit].sql, "SELECT ?")
  }

  test("Put syntaxt should convert an option to a fragment") {
    assertEquals((fr"SELECT" ++ Some(1).fr).query[Unit].sql, "SELECT ? ")
  }

  test("Put syntaxt should convert an option to a fragment0") {
    assertEquals((fr"SELECT" ++ Some(1).fr0).query[Unit].sql, "SELECT ?")
  }

  test("Put syntaxt should work in a map") {
    assertEquals(List(1, 2, 3).foldMap(_.fr).query[Unit].sql, "? ? ? ")
  }

  test("Put syntaxt should work in a map with fr0") {
    assertEquals(List(1, 2, 3).foldMap(_.fr0).query[Unit].sql, "???")
  }

}
