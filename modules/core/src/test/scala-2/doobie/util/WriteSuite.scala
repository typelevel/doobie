// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie
package util

import shapeless._, shapeless.record._

class WriteSuite extends munit.FunSuite {

  case class Woozle(a: (String, Int), b: Int :: String :: HNil, c: Boolean)

  case class LenStr1(n: Int, s: String)

  case class LenStr2(n: Int, s: String)
  object LenStr2 {
    implicit val LenStrMeta: Meta[LenStr2] =
      Meta[String].timap(s => LenStr2(s.length, s))(_.s)
  }

  test("Write should exist for some fancy types") {
    util.Write[Int]
    util.Write[(Int, Int)]
    util.Write[(Int, Int, String)]
    util.Write[(Int, (Int, String))]
    util.Write[Woozle]

    // https://github.com/tpolecat/doobie/pull/126 was reverted because these
    // derivations were failing with SOE
    util.Write[(Woozle, String)]
    util.Write[(Int, Woozle :: Woozle :: String :: HNil)]
  }

  test("Write should exist for Unit") {
    util.Write[Unit]
    assertEquals(util.Write[(Int, Unit)].length, 1)
  }

  test("Write should exist for shapeless record types") {

    type DL = (Double, Long)
    type A  = Record.`'foo -> Int, 'bar -> String, 'baz -> DL, 'quz -> Woozle`.T

    util.Write[A]
    util.Write[(A, A)]
  }

  test("Write should exist for option of some fancy types") {
    util.Write[Option[Int]]
    util.Write[Option[(Int, Int)]]
    util.Write[Option[(Int, Int, String)]]
    util.Write[Option[(Int, (Int, String))]]
    util.Write[Option[(Int, Option[(Int, String)])]]
    util.Write[Option[Woozle]]
    util.Write[Option[(Woozle, String)]]
    util.Write[Option[(Int, Woozle :: Woozle :: String :: HNil)]]
  }

  test("Write should exist for option of Unit") {
    util.Write[Option[Unit]]
    assertEquals(util.Write[Option[(Int, Unit)]].length, 1)
  }

  test("Write should select multi-column instance by default") {
    assertEquals(util.Write[LenStr1].length, 2)
  }

  test("Write should select 1-column instance when available") {
    assertEquals(util.Write[LenStr2].length, 1)
  }

}
