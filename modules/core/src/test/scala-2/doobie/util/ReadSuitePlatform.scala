// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import doobie.testutils.VoidExtensions
import shapeless.*
import shapeless.record.*

import scala.annotation.nowarn

trait ReadSuitePlatform { self: munit.FunSuite =>
  import doobie.generic.auto._

  test("Read should exist for shapeless record types") {
    type DL = (Double, Long) // used below
    type A = Record.`'foo -> Int, 'bar -> String, 'baz -> DL, 'quz -> Woozle`.T
    Read[A].void
    Read[(A, A)].void
  }: @nowarn("msg=.*DL is never used.*")

  case class Woozle(a: (String, Int), b: Int :: String :: HNil, c: Boolean)

  test("Read should exist for some fancy types") {
    Read[Woozle].void
    Read[(Woozle, String)].void
    Read[(Int, Woozle :: Woozle :: String :: HNil)].void
  }

  test("Read should exist for option of some fancy types") {
    Read[Option[Woozle]].void
    Read[Option[(Woozle, String)]].void
    Read[Option[(Int, Woozle :: Woozle :: String :: HNil)]].void
  }

}
