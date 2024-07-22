// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import Predef.augmentString
import doobie.testutils.*

trait ReadSuitePlatform { self: munit.FunSuite =>

  case class Woozle(a: (String, Int), b: Int *: String *: EmptyTuple, c: Boolean)

  sealed trait NoGetInstanceForThis
  case class CaseClassWithFieldWithoutGetInstance(a: String, b: NoGetInstanceForThis)

  test("Read should exist for some fancy types") {
    import doobie.generic.auto._

    Read[Woozle].void
    Read[(Woozle, String)].void
    Read[(Int, Woozle *: Woozle *: String *: EmptyTuple)].void
  }

  test("Read should exist for option of some fancy types") {
    import doobie.generic.auto._

    Read[Option[Woozle]].void
    Read[Option[(Woozle, String)]].void
    Read[Option[(Int, Woozle *: Woozle *: String *: EmptyTuple)]].void
  }

  test("Read should not exist for case class with field without Get instance") {
    val compileError = compileErrors("Read[CaseClassWithFieldWithoutGetInstance]")
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
    case class Foo(a: String, b: Int) derives Read
    val _ = Foo("", 1)
  }
}
