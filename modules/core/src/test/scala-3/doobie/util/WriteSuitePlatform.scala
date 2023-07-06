// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie
package util

import Predef.augmentString

trait WriteSuitePlatform { self: munit.FunSuite =>

  case class Woozle(a: (String, Int), b: Int *: String *: EmptyTuple, c: Boolean)
  
  sealed trait NoPutInstanceForThis
  case class CaseClassWithFieldWithoutPutInstance(a: String, b: NoPutInstanceForThis)

  test("Write should exist for some fancy types") {
    util.Write[Woozle]
    util.Write[(Woozle, String)]
    util.Write[(Int, Woozle *: Woozle *: String *: EmptyTuple)]
  }

  test("Write should exist for option of some fancy types") {
    util.Write[Option[Woozle]]
    util.Write[Option[(Woozle, String)]]
    util.Write[Option[(Int, Woozle *: Woozle *: String *: EmptyTuple)]]
  }

  test("Write should not exist for case class with field without Put instance") {
    val compileError = compileErrors("util.Write[CaseClassWithFieldWithoutPutInstance]")
    assert(
      compileError.contains(
        """Cannot find or construct a Write instance for type:
          |
          |  WriteSuitePlatform.this.CaseClassWithFieldWithoutPutInstance
          |
          |This can happen for a few reasons,""".stripMargin
      )
    )
  }

  test("derives") {
    case class Foo(a: String, b: Int) derives util.Write
  }
}
