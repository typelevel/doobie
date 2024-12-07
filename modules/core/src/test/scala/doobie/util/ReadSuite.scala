// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.IO
import doobie.util.TestTypes.*
import doobie.util.transactor.Transactor
import doobie.testutils.VoidExtensions
import doobie.syntax.all.*
import doobie.Query
import munit.Location
import scala.annotation.nowarn

class ReadSuite extends munit.FunSuite with ReadSuitePlatform {

  import cats.effect.unsafe.implicits.global

  val xa = Transactor.fromDriverManager[IO](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None
  )

  test("Read is available for tuples without an import when all elements have a Write instance") {
    Read[(Int, Int)].void
    Read[(Int, Int, String)].void
    Read[(Int, (Int, String))].void

    Read[Option[(Int, Int)]].void
    Read[Option[(Int, Option[(String, Int)])]].void

    // But shouldn't automatically derive anything that doesn't already have a Read instance
    assert(compileErrors("Read[(Int, TrivialCaseClass)]").contains("Cannot find or construct"))
  }

  test("Read is still auto derived for tuples when import is present (no ambiguous implicits) ") {
    import doobie.generic.auto.*
    Read[(Int, Int)].void
    Read[(Int, Int, String)].void
    Read[(Int, (Int, String))].void

    Read[Option[(Int, Int)]].void
    Read[Option[(Int, Option[(String, Int)])]].void

    Read[(ComplexCaseClass, Int)].void
    Read[(Int, ComplexCaseClass)].void
  }

  test("Read is not auto derived for case classes without importing auto derive import") {
    assert(compileErrors("Read[TrivialCaseClass]").contains("Cannot find or construct"))
  }

  test("Semiauto derivation selects custom Read instances when available") {
    implicit val i0: Read[HasCustomReadWrite0] = Read.derived[HasCustomReadWrite0]
    assertEquals(i0.length, 2)
    insertTupleAndCheckRead(("x", "y"), HasCustomReadWrite0(CustomReadWrite("x_R"), "y"))

    implicit val i1: Read[HasCustomReadWrite1] = Read.derived[HasCustomReadWrite1]
    assertEquals(i1.length, 2)
    insertTupleAndCheckRead(("x", "y"), HasCustomReadWrite1("x", CustomReadWrite("y_R")))

    implicit val iOpt0: Read[HasOptCustomReadWrite0] = Read.derived[HasOptCustomReadWrite0]
    assertEquals(iOpt0.length, 2)
    insertTupleAndCheckRead(("x", "y"), HasOptCustomReadWrite0(Some(CustomReadWrite("x_R")), "y"))

    implicit val iOpt1: Read[HasOptCustomReadWrite1] = Read.derived[HasOptCustomReadWrite1]
    assertEquals(iOpt1.length, 2)
    insertTupleAndCheckRead(("x", "y"), HasOptCustomReadWrite1("x", Some(CustomReadWrite("y_R"))))
  }

  test("Semiauto derivation selects custom Get instances to use for Read when available") {
    implicit val i0: Read[HasCustomGetPut0] = Read.derived[HasCustomGetPut0]
    assertEquals(i0.length, 2)
    insertTupleAndCheckRead(("x", "y"), HasCustomGetPut0(CustomGetPut("x_G"), "y"))

    implicit val i1: Read[HasCustomGetPut1] = Read.derived[HasCustomGetPut1]
    assertEquals(i1.length, 2)
    insertTupleAndCheckRead(("x", "y"), HasCustomGetPut1("x", CustomGetPut("y_G")))

    implicit val iOpt0: Read[HasOptCustomGetPut0] = Read.derived[HasOptCustomGetPut0]
    assertEquals(iOpt0.length, 2)
    insertTupleAndCheckRead(("x", "y"), HasOptCustomGetPut0(Some(CustomGetPut("x_G")), "y"))

    implicit val iOpt1: Read[HasOptCustomGetPut1] = Read.derived[HasOptCustomGetPut1]
    assertEquals(iOpt1.length, 2)
    insertTupleAndCheckRead(("x", "y"), HasOptCustomGetPut1("x", Some(CustomGetPut("y_G"))))
  }

  test("Automatic derivation selects custom Read instances when available") {
    import doobie.implicits.*

    insertTupleAndCheckRead(("x", "y"), HasCustomReadWrite0(CustomReadWrite("x_R"), "y"))
    insertTupleAndCheckRead(("x", "y"), HasCustomReadWrite1("x", CustomReadWrite("y_R")))
    insertTupleAndCheckRead(("x", "y"), HasOptCustomReadWrite0(Some(CustomReadWrite("x_R")), "y"))
    insertTupleAndCheckRead(("x", "y"), HasOptCustomReadWrite1("x", Some(CustomReadWrite("y_R"))))
  }

  test("Automatic derivation selects custom Get instances to use for Read when available") {
    import doobie.implicits.*
    insertTupleAndCheckRead(("x", "y"), HasCustomGetPut0(CustomGetPut("x_G"), "y"))
    insertTupleAndCheckRead(("x", "y"), HasCustomGetPut1("x", CustomGetPut("y_G")))
    insertTupleAndCheckRead(("x", "y"), HasOptCustomGetPut0(Some(CustomGetPut("x_G")), "y"))
    insertTupleAndCheckRead(("x", "y"), HasOptCustomGetPut1("x", Some(CustomGetPut("y_G"))))
  }

  test("Read should not be derivable for case objects") {
    val expectedDeriveError =
      if (util.Properties.versionString.startsWith("version 2.12"))
        "could not find implicit"
      else
        "Cannot derive"
    assert(compileErrors("Read.derived[CaseObj.type]").contains(expectedDeriveError))
    assert(compileErrors("Read.derived[Option[CaseObj.type]]").contains(expectedDeriveError))

    import doobie.implicits.*
    assert(compileErrors("Read[CaseObj.type]").contains("not find or construct"))
    assert(compileErrors("Read[Option[CaseObj.type]]").contains("not find or construct"))
  }: @nowarn("msg=.*(u|U)nused import.*")

  test("Read should exist for Unit/Option[Unit]") {
    assertEquals(Read[Unit].length, 0)
    assertEquals(Read[Option[Unit]].length, 0)
    assertEquals(Read[(Int, Unit)].length, 1)
  }

  test(".product should product the correct ordering of gets") {
    import cats.syntax.all.*

    val readInt = Read[Int]
    val readString = Read[String]

    val p = readInt.product(readString)

    assertEquals(p.gets, (readInt.gets ++ readString.gets))
  }

  /*
  case class with nested Option case class field
   */

  test("Read should read correct columns for instances with Option (None)") {
    import doobie.implicits.*

    val frag = sql"SELECT 1, NULL, 3, NULL"
    val q1 = frag.query[Option[(Int, Option[Int], Int, Option[Int])]].to[List]
    val o1 = q1.transact(xa).unsafeRunSync()
    assertEquals(o1, List(Some((1, None, 3, None))))

    val q2 = frag.query[Option[(Int, Int, Int, Int)]].to[List]
    val o2 = q2.transact(xa).unsafeRunSync()
    assertEquals(o2, List(None))
  }

  test("Read should read correct columns for instances with Option (Some)") {
    import doobie.implicits.*

    val frag = sql"SELECT 1, 2, 3, 4"
    val q1 = frag.query[Option[(Int, Option[Int], Int, Option[Int])]].to[List]
    val o1 = q1.transact(xa).unsafeRunSync()
    assertEquals(o1, List(Some((1, Some(2), 3, Some(4)))))

    val q2 = frag.query[Option[(Int, Int, Int, Int)]].to[List]
    val o2 = q2.transact(xa).unsafeRunSync()
    assertEquals(o2, List(Some((1, 2, 3, 4))))
  }

  test("Read should select correct columns when combined with `ap`") {
    import cats.syntax.all.*
    import doobie.implicits.*

    val r = Read[Int]

    val c = (r, r, r, r, r).tupled

    val q = sql"SELECT 1, 2, 3, 4, 5".query(using c).to[List]

    val o = q.transact(xa).unsafeRunSync()

    assertEquals(o, List((1, 2, 3, 4, 5)))
  }

  test("Read should select correct columns when combined with `product`") {
    import cats.syntax.all.*
    import doobie.implicits.*

    val r = Read[Int].product(Read[Int].product(Read[Int]))

    val q = sql"SELECT 1, 2, 3".query(using r).to[List]
    val o = q.transact(xa).unsafeRunSync()

    assertEquals(o, List((1, (2, 3))))
  }

  private def insertTupleAndCheckRead[Tup: Write, A: Read](in: Tup, expectedOut: A)(implicit loc: Location): Unit = {
    val res = Query[Tup, A]("SELECT ?, ?").unique(in).transact(xa)
      .unsafeRunSync()
    assertEquals(res, expectedOut)
  }

}
