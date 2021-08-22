// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util.meta

import doobie.util.{Get, Put}

import scala.annotation.nowarn


@nowarn("msg=.*never used.*")
class MetaSuite extends munit.FunSuite {

  test("Meta should exist for primitive types") {
    Meta[Int]
    Meta[String]
  }

  test("Meta should imply Get") {
    def foo[A: Meta] = Get[A]
  }

  test("Meta should imply Put") {
    def foo[A: Meta] = Put[A]
  }

}
