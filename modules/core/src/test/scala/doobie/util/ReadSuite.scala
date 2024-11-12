// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.IO
import doobie.util.TestTypes.*
import doobie.util.transactor.Transactor
import doobie.testutils.VoidExtensions
import doobie.syntax.all.*
import doobie.{ConnectionIO, Query}
import doobie.util.analysis.{Analysis, ColumnMisalignment, ColumnTypeError, ColumnTypeWarning, NullabilityMisalignment}
import doobie.util.fragment.Fragment
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
    insertTuple2AndCheckRead(("x", "y"), HasCustomReadWrite0(CustomReadWrite("x_R"), "y"))

    implicit val i1: Read[HasCustomReadWrite1] = Read.derived[HasCustomReadWrite1]
    assertEquals(i1.length, 2)
    insertTuple2AndCheckRead(("x", "y"), HasCustomReadWrite1("x", CustomReadWrite("y_R")))

    implicit val iOpt0: Read[HasOptCustomReadWrite0] = Read.derived[HasOptCustomReadWrite0]
    assertEquals(iOpt0.length, 2)
    insertTuple2AndCheckRead(("x", "y"), HasOptCustomReadWrite0(Some(CustomReadWrite("x_R")), "y"))

    implicit val iOpt1: Read[HasOptCustomReadWrite1] = Read.derived[HasOptCustomReadWrite1]
    assertEquals(iOpt1.length, 2)
    insertTuple2AndCheckRead(("x", "y"), HasOptCustomReadWrite1("x", Some(CustomReadWrite("y_R"))))
  }

  test("Semiauto derivation selects custom Get instances to use for Read when available") {
    implicit val i0: Read[HasCustomGetPut0] = Read.derived[HasCustomGetPut0]
    assertEquals(i0.length, 2)
    insertTuple2AndCheckRead(("x", "y"), HasCustomGetPut0(CustomGetPut("x_G"), "y"))

    implicit val i1: Read[HasCustomGetPut1] = Read.derived[HasCustomGetPut1]
    assertEquals(i1.length, 2)
    insertTuple2AndCheckRead(("x", "y"), HasCustomGetPut1("x", CustomGetPut("y_G")))

    implicit val iOpt0: Read[HasOptCustomGetPut0] = Read.derived[HasOptCustomGetPut0]
    assertEquals(iOpt0.length, 2)
    insertTuple2AndCheckRead(("x", "y"), HasOptCustomGetPut0(Some(CustomGetPut("x_G")), "y"))

    implicit val iOpt1: Read[HasOptCustomGetPut1] = Read.derived[HasOptCustomGetPut1]
    assertEquals(iOpt1.length, 2)
    insertTuple2AndCheckRead(("x", "y"), HasOptCustomGetPut1("x", Some(CustomGetPut("y_G"))))
  }

  test("Automatic derivation selects custom Read instances when available") {
    import doobie.implicits.*

    insertTuple2AndCheckRead(("x", "y"), HasCustomReadWrite0(CustomReadWrite("x_R"), "y"))
    insertTuple2AndCheckRead(("x", "y"), HasCustomReadWrite1("x", CustomReadWrite("y_R")))
    insertTuple2AndCheckRead(("x", "y"), HasOptCustomReadWrite0(Some(CustomReadWrite("x_R")), "y"))
    insertTuple2AndCheckRead(("x", "y"), HasOptCustomReadWrite1("x", Some(CustomReadWrite("y_R"))))
  }

  test("Automatic derivation selects custom Get instances to use for Read when available") {
    import doobie.implicits.*
    insertTuple2AndCheckRead(("x", "y"), HasCustomGetPut0(CustomGetPut("x_G"), "y"))
    insertTuple2AndCheckRead(("x", "y"), HasCustomGetPut1("x", CustomGetPut("y_G")))
    insertTuple2AndCheckRead(("x", "y"), HasOptCustomGetPut0(Some(CustomGetPut("x_G")), "y"))
    insertTuple2AndCheckRead(("x", "y"), HasOptCustomGetPut1("x", Some(CustomGetPut("y_G"))))
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

  test(".map should correctly transform the value") {
    import doobie.implicits.*
    implicit val r: Read[WrappedSimpleCaseClass] = Read[SimpleCaseClass].map(s =>
      WrappedSimpleCaseClass(
        s.copy(s = "custom")
      ))

    insertTuple3AndCheckRead((1, "s1", "s2"), WrappedSimpleCaseClass(SimpleCaseClass(Some(1), "custom", Some("s2"))))
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

  test("Read typechecking should work for Tuples") {
    val frag = sql"SELECT 1, 's', 3.0 :: DOUBLE"

    assertSuccessTypecheckRead[(Int, String, Double)](frag)
    assertSuccessTypecheckRead[(Int, (String, Double))](frag)
    assertSuccessTypecheckRead[((Int, String), Double)](frag)

    assertSuccessTypecheckRead[(Int, Option[String], Double)](frag)
    assertSuccessTypecheckRead[(Option[Int], Option[(String, Double)])](frag)
    assertSuccessTypecheckRead[Option[((Int, String), Double)]](frag)

    assertWarnedTypecheckRead[(Boolean, String, Double)](frag)

    assertMisalignedTypecheckRead[(Int, String)](frag)
    assertMisalignedTypecheckRead[(Int, String, Double, Int)](frag)

  }

  test("Read typechecking should work for case classes") {
    implicit val rscc: Read[SimpleCaseClass] = Read.derived[SimpleCaseClass]
    implicit val rccc: Read[ComplexCaseClass] = Read.derived[ComplexCaseClass]
    implicit val rwscc: Read[WrappedSimpleCaseClass] =
      rscc.map(WrappedSimpleCaseClass.apply) // Test map doesn't break typechecking

    assertSuccessTypecheckRead(
      sql"create table tab(c1 int, c2 varchar not null, c3 varchar)".update.run.flatMap(_ =>
        sql"SELECT c1,c2,c3 from tab".query[SimpleCaseClass].analysis)
    )
    assertSuccessTypecheckRead(
      sql"create table tab(c1 int, c2 varchar not null, c3 varchar)".update.run.flatMap(_ =>
        sql"SELECT c1,c2,c3 from tab".query[WrappedSimpleCaseClass].analysis)
    )

    assertSuccessTypecheckRead(
      sql"create table tab(c1 int, c2 varchar, c3 varchar)".update.run.flatMap(_ =>
        sql"SELECT c1,c2,c3 from tab".query[Option[SimpleCaseClass]].analysis)
    )
    assertSuccessTypecheckRead(
      sql"create table tab(c1 int, c2 varchar, c3 varchar)".update.run.flatMap(_ =>
        sql"SELECT c1,c2,c3 from tab".query[Option[WrappedSimpleCaseClass]].analysis)
    )

    assertTypeErrorTypecheckRead(
      sql"create table tab(c1 binary, c2 varchar not null, c3 varchar)".update.run.flatMap(_ =>
        sql"SELECT c1,c2,c3 from tab".query[SimpleCaseClass].analysis)
    )

    assertMisalignedNullabilityTypecheckRead(
      sql"create table tab(c1 int, c2 varchar, c3 varchar)".update.run.flatMap(_ =>
        sql"SELECT c1,c2,c3 from tab".query[SimpleCaseClass].analysis)
    )

    assertSuccessTypecheckRead(
      sql"create table tab(c1 int, c2 varchar not null, c3 varchar, c4 int, c5 varchar, c6 varchar, c7 int, c8 varchar not null)"
        .update.run.flatMap(_ =>
          sql"SELECT c1,c2,c3,c4,c5,c6,c7,c8 from tab".query[ComplexCaseClass].analysis)
    )

    assertTypeErrorTypecheckRead(
      sql"create table tab(c1 binary, c2 varchar not null, c3 varchar, c4 int, c5 varchar, c6 varchar, c7 int, c8 varchar not null)"
        .update.run.flatMap(_ =>
          sql"SELECT c1,c2,c3,c4,c5,c6,c7,c8 from tab".query[ComplexCaseClass].analysis)
    )

    assertMisalignedNullabilityTypecheckRead(
      sql"create table tab(c1 int, c2 varchar, c3 varchar, c4 int, c5 varchar, c6 varchar, c7 int, c8 varchar not null)"
        .update.run.flatMap(_ =>
          sql"SELECT c1,c2,c3,c4,c5,c6,c7,c8 from tab".query[ComplexCaseClass].analysis)
    )

  }

  private def insertTuple3AndCheckRead[Tup <: (?, ?, ?): Write, A: Read](in: Tup, expectedOut: A)(implicit
      loc: Location
  ): Unit = {
    val res = Query[Tup, A]("SELECT ?, ?, ?").unique(in).transact(xa)
      .unsafeRunSync()
    assertEquals(res, expectedOut)
  }

  private def insertTuple2AndCheckRead[Tup <: (?, ?): Write, A: Read](in: Tup, expectedOut: A)(implicit
      loc: Location
  ): Unit = {
    val res = Query[Tup, A]("SELECT ?, ?").unique(in).transact(xa)
      .unsafeRunSync()
    assertEquals(res, expectedOut)
  }

  private def assertSuccessTypecheckRead(connio: ConnectionIO[Analysis])(implicit loc: Location): Unit = {
    val analysisResult = connio.transact(xa).unsafeRunSync()
    assertEquals(analysisResult.columnAlignmentErrors, Nil)
  }

  private def assertSuccessTypecheckRead[A: Read](frag: Fragment)(implicit loc: Location): Unit = {
    assertSuccessTypecheckRead(frag.query[A].analysis)
  }

  private def assertWarnedTypecheckRead[A: Read](frag: Fragment)(implicit loc: Location): Unit = {
    val analysisResult = frag.query[A].analysis.transact(xa).unsafeRunSync()
    val errorClasses = analysisResult.columnAlignmentErrors.map(_.getClass)
    assertEquals(errorClasses, List(classOf[ColumnTypeWarning]))
  }

  private def assertTypeErrorTypecheckRead(connio: ConnectionIO[Analysis])(implicit loc: Location): Unit = {
    val analysisResult = connio.transact(xa).unsafeRunSync()
    val errorClasses = analysisResult.columnAlignmentErrors.map(_.getClass)
    assertEquals(errorClasses, List(classOf[ColumnTypeError]))
  }

  private def assertMisalignedNullabilityTypecheckRead(connio: ConnectionIO[Analysis])(implicit loc: Location): Unit = {
    val analysisResult = connio.transact(xa).unsafeRunSync()
    val errorClasses = analysisResult.columnAlignmentErrors.map(_.getClass)
    assertEquals(errorClasses, List(classOf[NullabilityMisalignment]))
  }

  private def assertMisalignedTypecheckRead[A: Read](frag: Fragment)(implicit loc: Location): Unit = {
    val analysisResult = frag.query[A].analysis.transact(xa).unsafeRunSync()
    val errorClasses = analysisResult.columnAlignmentErrors.map(_.getClass)
    assertEquals(errorClasses, List(classOf[ColumnMisalignment]))
  }

}
