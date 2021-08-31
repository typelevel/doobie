// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie
package util

import cats.effect.IO

class ReadSuite extends munit.FunSuite with ReadSuitePlatform {

  import cats.effect.unsafe.implicits.global

  case class LenStr1(n: Int, s: String)

  case class LenStr2(n: Int, s: String)
  object LenStr2 {
    implicit val LenStrMeta: Meta[LenStr2] =
      Meta[String].timap(s => LenStr2(s.length, s))(_.s)
  }

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  test("Read should exist for some fancy types") {
    util.Read[Int]
    util.Read[(Int, Int)]
    util.Read[(Int, Int, String)]
    util.Read[(Int, (Int, String))]
  }

  test("Read should exist for Unit") {
    util.Read[Unit]
    assertEquals(util.Read[(Int, Unit)].length, 1)
  }

  test("Read should exist for option of some fancy types") {
    util.Read[Option[Int]]
    util.Read[Option[(Int, Int)]]
    util.Read[Option[(Int, Int, String)]]
    util.Read[Option[(Int, (Int, String))]]
    util.Read[Option[(Int, Option[(Int, String)])]]
  }

  test("Read should exist for option of Unit") {
    util.Read[Option[Unit]]
    assertEquals(util.Read[Option[(Int, Unit)]].length, 1)
  }

  test("Read should select multi-column instance by default") {
    assertEquals(util.Read[LenStr1].length, 2)
  }

  test("Read should select 1-column instance when available") {
    assertEquals(util.Read[LenStr2].length, 1)
  }

  test(".product should product the correct ordering of gets") {
    import cats.syntax.all._

    val readInt = util.Read[Int]
    val readString = util.Read[String]

    val p = readInt.product(readString)

    assertEquals(p.gets, (readInt.gets ++ readString.gets))
  }

  test("Read should select correct columns when combined with `ap`") {
    import cats.syntax.all._
    import doobie.implicits._

    val r = util.Read[Int]

    val c = (r, r, r, r, r).tupled

    val q = sql"SELECT 1, 2, 3, 4, 5".query(c).to[List]

    val o = q.transact(xa).unsafeRunSync()

    assertEquals(o, List((1, 2, 3, 4, 5)))
  }

  test("Read should select correct columns when combined with `product`") {
    import cats.syntax.all._
    import doobie.implicits._
    val r = util.Read[Int].product(util.Read[Int].product(util.Read[Int]))

    val q = sql"SELECT 1, 2, 3".query(r).to[List]
    val o = q.transact(xa).unsafeRunSync()

    assertEquals(o, List((1, (2, 3))))
  }

}
