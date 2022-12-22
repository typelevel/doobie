// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie
package util

import shapeless._
import shapeless.record._

import scala.annotation.nowarn

trait ReadSuitePlatform { self: munit.FunSuite =>

  test("Read should exist for shapeless record types") {
    type DL = (Double, Long) // used below
    type A  = Record.`'foo -> Int, 'bar -> String, 'baz -> DL, 'quz -> Woozle`.T
    util.Read[A]
    util.Read[(A, A)]
  }: @nowarn("msg=.*DL is never used.*")
  
  case class Woozle(a: (String, Int), b: Int :: String :: HNil, c: Boolean)

  test("Read should exist for some fancy types") {
    util.Read[Woozle]
    util.Read[(Woozle, String)]
    util.Read[(Int, Woozle :: Woozle :: String :: HNil)]
  }

  test("Read should exist for option of some fancy types") {
    util.Read[Option[Woozle]]
    util.Read[Option[(Woozle, String)]]
    util.Read[Option[(Int, Woozle :: Woozle :: String :: HNil)]]
  }

}
