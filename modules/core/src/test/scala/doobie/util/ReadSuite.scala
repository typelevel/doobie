// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.IO
import doobie.util.TestTypes.*
import doobie.util.transactor.Transactor
import doobie.testutils.VoidExtensions
import munit.CatsEffectSuite

class ReadSuite extends CatsEffectSuite with ReadSuitePlatform {
  
  val xa = Transactor.fromDriverManager[IO](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None
  )

  test("Read should exist for some fancy types") {
    import doobie.generic.auto.*

    Read[Int].void
    Read[(Int, Int)].void
    Read[(Int, Int, String)].void
    Read[(Int, (Int, String))].void
  }

  test("Read is not auto derived for case classes without importing auto derive import") {
    assert(compileErrors("Read[LenStr1]").contains("Cannot find or construct"))
  }

  test("Read should not be derivable for case objects") {
    assert(compileErrors("Read[CaseObj.type]").contains("Cannot find or construct"))
    assert(compileErrors("Read[Option[CaseObj.type]]").contains("Cannot find or construct"))
  }

  test("Read is auto derived for tuples without an import") {
    Read[(Int, Int)].void
    Read[(Int, Int, String)].void
    Read[(Int, (Int, String))].void

    Read[Option[(Int, Int)]].void
    Read[Option[(Int, Option[(String, Int)])]].void
  }

  test("Read is still auto derived for tuples when import is present (no ambiguous implicits)") {
    import doobie.generic.auto.*
    Read[(Int, Int)].void
    Read[(Int, Int, String)].void
    Read[(Int, (Int, String))].void

    Read[Option[(Int, Int)]].void
    Read[Option[(Int, Option[(String, Int)])]].void
  }

  test("Read can be manually derived") {
    Read.derived[LenStr1]
  }

  test("Read should exist for Unit") {
    import doobie.generic.auto.*

    Read[Unit]
    assertEquals(Read[(Int, Unit)].length, 1)
  }

  test("Read should exist for option of some fancy types") {
    import doobie.generic.auto.*

    Read[Option[Int]].void
    Read[Option[(Int, Int)]].void
    Read[Option[(Int, Int, String)]].void
    Read[Option[(Int, (Int, String))]].void
    Read[Option[(Int, Option[(Int, String)])]].void
    Read[ComplexCaseClass].void
  }

  test("Read should exist for option of Unit") {
    import doobie.generic.auto.*

    Read[Option[Unit]].void
    assertEquals(Read[Option[(Int, Unit)]].length, 1).void
  }

  test("Read should select multi-column instance by default") {
    import doobie.generic.auto.*

    assertEquals(Read[LenStr1].length, 2).void
  }

  test("Read should select 1-column instance when available") {
    assertEquals(Read[LenStr2].length, 1).void
  }

  test(".product should product the correct ordering of gets") {
    import cats.syntax.all.*

    val readInt = Read[Int]
    val readString = Read[String]

    val p = readInt.product(readString)

    assertEquals(p.gets, readInt.gets ++ readString.gets)
  }

  /*
  case class with nested Option case class field
   */

  test("Read should read correct columns for instances with Option (None)") {
    import doobie.implicits.*

    val frag = sql"SELECT 1, NULL, 3, NULL"
    val q1 = frag.query[Option[(Int, Option[Int], Int, Option[Int])]].to[List]
    // This result doesn't seem ideal, because we should know that Int isn't
    // nullable, so the correct result is Some((1, None, 3, None))
    // But with how things are wired at the moment this isn't possible
    q1.transact(xa).assertEquals(List(None))

    val q2 = frag.query[Option[(Int, Int, Int, Int)]].to[List]
    q2.transact(xa).assertEquals(List(None))
  }

  test("Read should read correct columns for instances with Option (Some)") {
    import doobie.implicits.*

    val frag = sql"SELECT 1, 2, 3, 4"
    val q1 = frag.query[Option[(Int, Option[Int], Int, Option[Int])]].to[List]
    q1.transact(xa).assertEquals(List(Some((1, Some(2), 3, Some(4)))))

    val q2 = frag.query[Option[(Int, Int, Int, Int)]].to[List]
    q2.transact(xa).assertEquals(List(Some((1, 2, 3, 4))))
  }

  test("Read should select correct columns when combined with `ap`") {
    import cats.syntax.all.*
    import doobie.implicits.*

    val r = Read[Int]

    val c = (r, r, r, r, r).tupled

    val q = sql"SELECT 1, 2, 3, 4, 5".query(using c).to[List]
    q.transact(xa).assertEquals(List((1, 2, 3, 4, 5)))
  }

  test("Read should select correct columns when combined with `product`") {
    import cats.syntax.all.*
    import doobie.implicits.*

    val r = Read[Int].product(Read[Int].product(Read[Int]))

    val q = sql"SELECT 1, 2, 3".query(using r).to[List]
    q.transact(xa).assertEquals(List((1, (2, 3))))
  }

}
