// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie
package util

import Predef.augmentString

trait ReadSuitePlatform { self: munit.FunSuite =>

  case class Woozle(a: (String, Int), b: Int *: String *: EmptyTuple, c: Boolean)
  
  sealed trait NoGetInstanceForThis
  case class CaseClassWithFieldWithoutGetInstance(a: String, b: NoGetInstanceForThis)

  test("Read should exist for some fancy types") {
    util.Read[Woozle]
    util.Read[(Woozle, String)]
    util.Read[(Int, Woozle *: Woozle *: String *: EmptyTuple)]
  }

  test("Read should exist for option of some fancy types") {
    util.Read[Option[Woozle]]
    util.Read[Option[(Woozle, String)]]
    util.Read[Option[(Int, Woozle *: Woozle *: String *: EmptyTuple)]]
  }

  test("Read should not exist for case class with field without Get instance") {
    val compileError = compileErrors("util.Read[CaseClassWithFieldWithoutGetInstance]")
    assert(
      compileError.contains(
        """Cannot find or construct a Read instance for type:
          |
          |  ReadSuitePlatform.this.CaseClassWithFieldWithoutGetInstance
          |
          |This can happen for a few reasons,""".stripMargin
      )
    )
  }

  test("derives") {
    case class Foo(a: String, b: Int) derives util.Read
  }
}
