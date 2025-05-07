// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import shapeless.*
import shapeless.record.*
import doobie.testutils.VoidExtensions

import scala.annotation.nowarn

trait WriteSuitePlatform { self: munit.FunSuite =>
  import doobie.generic.auto.*

  test("Write should exist for shapeless record types") {
    type DL = (Double, Long)
    type A = Record.`'foo -> Int, 'bar -> String, 'baz -> DL, 'quz -> Woozle`.T
    Write[A].void
    Write[(A, A)].void
  }: @nowarn("msg=.*DL is never used.*")

  case class Woozle(a: (String, Int), b: Int :: String :: HNil, c: Boolean)

  test("Write should exist for some fancy types") {
    Write[Woozle].void
    Write[(Woozle, String)].void
    Write[(Int, Woozle :: Woozle :: String :: HNil)].void
  }

  test("Write should exist for option of some fancy types") {
    Write[Option[Woozle]].void
    Write[Option[(Woozle, String)]].void
    Write[Option[(Int, Woozle :: Woozle :: String :: HNil)]].void
  }

}
