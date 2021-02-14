// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie
package util

trait ReadSuitePlatform { self: munit.FunSuite =>

  case class Woozle(a: (String, Int), b: (Int, String), c: Boolean)

  test("Read should exist for some fancy types") {
    util.Read[Woozle]
    util.Read[(Woozle, String)]
    util.Read[(Int, (Woozle, Woozle, String))]
  }

  test("Read should exist for option of some fancy types") {
    util.Read[Option[Woozle]]
    util.Read[Option[(Woozle, String)]]
    util.Read[Option[(Int, (Woozle, Woozle, String))]]
  }

}
