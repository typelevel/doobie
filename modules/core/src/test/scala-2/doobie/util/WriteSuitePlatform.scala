// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie
package util

import shapeless._
import shapeless.record._

import scala.annotation.nowarn


trait WriteSuitePlatform { self: munit.FunSuite =>

  test("Write should exist for shapeless record types") {
    type DL = (Double, Long)
    type A  = Record.`'foo -> Int, 'bar -> String, 'baz -> DL, 'quz -> Woozle`.T
    util.Write[A]
    util.Write[(A, A)]
  }: @nowarn("msg=.*DL is never used.*")

  case class Woozle(a: (String, Int), b: Int :: String :: HNil, c: Boolean)

  test("Write should exist for some fancy types") {
    util.Write[Woozle]
    util.Write[(Woozle, String)]
    util.Write[(Int, Woozle :: Woozle :: String :: HNil)]
  }

  test("Write should exist for option of some fancy types") {
    util.Write[Option[Woozle]]
    util.Write[Option[(Woozle, String)]]
    util.Write[Option[(Int, Woozle :: Woozle :: String :: HNil)]]
  }

}
