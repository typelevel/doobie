// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

trait ReadSuitePlatform { self: munit.FunSuite =>

  case class Woozle(a: (String, Int), b: Int *: String *: EmptyTuple, c: Boolean)

  test("Read should exist for some fancy types") {
    import doobie.generic.auto._

    Read[Woozle]
    Read[(Woozle, String)]
    Read[(Int, Woozle *: Woozle *: String *: EmptyTuple)]
  }

  test("Read should exist for option of some fancy types") {
    import doobie.generic.auto._

    Read[Option[Woozle]]
    Read[Option[(Woozle, String)]]
    Read[Option[(Int, Woozle *: Woozle *: String *: EmptyTuple)]]
  }

  test("derives") {
    case class Foo(a: String, b: Int) derives Read
  }
}
