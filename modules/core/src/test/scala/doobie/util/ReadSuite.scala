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
import ReadSuite.*

import scala.annotation.nowarn

class ReadSuite extends munit.CatsEffectSuite with ReadSuitePlatform {

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
    implicit val i1: Read[HasCustomReadWrite1] = Read.derived[HasCustomReadWrite1]
    implicit val iOpt0: Read[HasOptCustomReadWrite0] = Read.derived[HasOptCustomReadWrite0]
    implicit val iOpt1: Read[HasOptCustomReadWrite1] = Read.derived[HasOptCustomReadWrite1]

    IO { assertEquals(i0.length, 2) } *>
      insertTuple2AndCheckRead(("x", "y"), HasCustomReadWrite0(CustomReadWrite("x_R"), "y")) *>
      IO { assertEquals(i1.length, 2) } *>
      insertTuple2AndCheckRead(("x", "y"), HasCustomReadWrite1("x", CustomReadWrite("y_R"))) *>
      IO { assertEquals(iOpt0.length, 2) } *>
      insertTuple2AndCheckRead(("x", "y"), HasOptCustomReadWrite0(Some(CustomReadWrite("x_R")), "y")) *>
      IO { assertEquals(iOpt1.length, 2) } *>
      insertTuple2AndCheckRead(("x", "y"), HasOptCustomReadWrite1("x", Some(CustomReadWrite("y_R"))))
  }

  test("Semiauto derivation selects custom Get instances to use for Read when available") {
    implicit val i0: Read[HasCustomGetPut0] = Read.derived[HasCustomGetPut0]
    implicit val i1: Read[HasCustomGetPut1] = Read.derived[HasCustomGetPut1]
    implicit val iOpt0: Read[HasOptCustomGetPut0] = Read.derived[HasOptCustomGetPut0]
    implicit val iOpt1: Read[HasOptCustomGetPut1] = Read.derived[HasOptCustomGetPut1]

    IO { assertEquals(i0.length, 2) } *>
      insertTuple2AndCheckRead(("x", "y"), HasCustomGetPut0(CustomGetPut("x_G"), "y")) *>
      IO { assertEquals(i1.length, 2) } *>
      insertTuple2AndCheckRead(("x", "y"), HasCustomGetPut1("x", CustomGetPut("y_G"))) *>
      IO { assertEquals(iOpt0.length, 2) } *>
      insertTuple2AndCheckRead(("x", "y"), HasOptCustomGetPut0(Some(CustomGetPut("x_G")), "y")) *>
      IO { assertEquals(iOpt1.length, 2) } *>
      insertTuple2AndCheckRead(("x", "y"), HasOptCustomGetPut1("x", Some(CustomGetPut("y_G"))))
  }

  test("Automatic derivation selects custom Read instances when available") {
    import doobie.implicits.*

    insertTuple2AndCheckRead(("x", "y"), HasCustomReadWrite0(CustomReadWrite("x_R"), "y")) *>
      insertTuple2AndCheckRead(("x", "y"), HasCustomReadWrite1("x", CustomReadWrite("y_R"))) *>
      insertTuple2AndCheckRead(("x", "y"), HasOptCustomReadWrite0(Some(CustomReadWrite("x_R")), "y")) *>
      insertTuple2AndCheckRead(("x", "y"), HasOptCustomReadWrite1("x", Some(CustomReadWrite("y_R"))))
  }

  test("Automatic derivation selects custom Get instances to use for Read when available") {
    import doobie.implicits.*
    insertTuple2AndCheckRead(("x", "y"), HasCustomGetPut0(CustomGetPut("x_G"), "y")) *>
      insertTuple2AndCheckRead(("x", "y"), HasCustomGetPut1("x", CustomGetPut("y_G"))) *>
      insertTuple2AndCheckRead(("x", "y"), HasOptCustomGetPut0(Some(CustomGetPut("x_G")), "y")) *>
      insertTuple2AndCheckRead(("x", "y"), HasOptCustomGetPut1("x", Some(CustomGetPut("y_G"))))
  }

  test("Read should not be derivable for case objects or empty case classes") {
    val expectedDeriveError =
      ScalaBinaryVersion.currentVersion match {
        case ScalaBinaryVersion.S2_12 => "could not find implicit"
        case ScalaBinaryVersion.S2_13 => "Cannot derive"
        case ScalaBinaryVersion.S3    => "Cannot derive"
      }
    assert(compileErrors("Read.derived[CaseObj.type]").contains(expectedDeriveError))
    assert(compileErrors("Read.derived[ZeroFieldCaseClass]").contains(expectedDeriveError))

    val expectedErrorWithAutoDerivationEnabled = ScalaBinaryVersion.currentVersion match {
      case ScalaBinaryVersion.S2_12 => "Cannot find or construct"
      case ScalaBinaryVersion.S2_13 => "Cannot find or construct"
      case ScalaBinaryVersion.S3    => "Cannot derive"
    }

    import doobie.implicits.*
    assert(compileErrors("Read[CaseObj.type]").contains(expectedErrorWithAutoDerivationEnabled))
    assert(compileErrors("Read[Option[CaseObj.type]]").contains(expectedErrorWithAutoDerivationEnabled))
    assert(compileErrors("Read[Option[ZeroFieldCaseClass]]").contains(expectedErrorWithAutoDerivationEnabled))
    assert(compileErrors("Read.derived[CaseObj.type]").contains(expectedDeriveError))
    assert(compileErrors("Read.derived[ZeroFieldCaseClass]").contains(expectedDeriveError))
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

    assertEquals(p.gets, readInt.gets ++ readString.gets)
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
    val q2 = frag.query[Option[(Int, Int, Int, Int)]].to[List]
    q1.transact(xa).assertEquals(List(Some((1, None, 3, None)))) *>
      q2.transact(xa).assertEquals(List(None))
  }

  test("Read should read correct columns for instances with Option (None) with left join between two tables") {

    import doobie.implicits.*

    (for {
      _ <- sql"drop table if exists animal".update.run
      _ <- sql"drop table if exists person".update.run
      _ <- sql"create table animal(id int not null, name varchar not null, owner_id int)".update.run
      _ <- sql"create table person(id int not null, name varchar)".update.run

      _ <- sql"insert into person values (101, 'Alice'), (102, 'Bob'), (999, NULL)".update.run
      _ <- sql"""insert into animal values 
             (1, 'Rex', 101), 
             (2, 'Whiskers needs no human', NULL), 
             (3, 'Pet whos owner is nameless', 999)
           """.update.run

      leftJoinWithRequiredColumns <-
        sql"""select animal.id, animal.name, person.id, person.name 
              from animal 
              left join person on animal.owner_id = person.id
              order by animal.id"""
          .query[(Animal, Option[Person])].to[List]
      _ = assertEquals(
        leftJoinWithRequiredColumns,
        List(
          (Animal(1, "Rex"), Some(Person(101, Some("Alice")))),
          (Animal(2, "Whiskers needs no human"), None),
          (Animal(3, "Pet whos owner is nameless"), Some(Person(999, None)))
        )
      )

      leftJoinWithOnlyOptionalColumns <-
        sql"""select animal.id, animal.name, person.name 
              from animal 
              left join person on animal.owner_id = person.id
              order by animal.id"""
          .query[(Animal, Option[PersonJustName])].to[List]

      _ = assertEquals(
        leftJoinWithOnlyOptionalColumns,
        List(
          (Animal(1, "Rex"), Some(PersonJustName(Some("Alice")))),
          // If you fetch an Option of all optional case class, you will always get a Some
          (Animal(2, "Whiskers needs no human"), Some(PersonJustName(None))),
          (Animal(3, "Pet whos owner is nameless"), Some(PersonJustName(None)))
        )
      )

      // Use custom Option instances for our case class to convert case when all fields are None
      leftJoinWithOnlyOptionalColumnsCustom <-
        sql"""select animal.id, animal.name, person.name 
              from animal 
              left join person on animal.owner_id = person.id
              order by animal.id"""
          .query[(Animal, Option[PersonJustNameCustomOption])].to[List]

      _ = assertEquals(
        leftJoinWithOnlyOptionalColumnsCustom,
        List(
          (Animal(1, "Rex"), Some(PersonJustNameCustomOption(Some("Alice")))),
          (Animal(2, "Whiskers needs no human"), None),
          (Animal(3, "Pet whos owner is nameless"), None)
        )
      )
    } yield ())
      .transact(xa)
  }

  test("Read should read correct columns for instances with Option (Some)") {
    import doobie.implicits.*

    val frag = sql"SELECT 1, 2, 3, 4"
    val q1 = frag.query[Option[(Int, Option[Int], Int, Option[Int])]].to[List]
    val q2 = frag.query[Option[(Int, Int, Int, Int)]].to[List]

    q1.transact(xa).assertEquals(List(Some((1, Some(2), 3, Some(4))))) *>
      q2.transact(xa).assertEquals(List(Some((1, 2, 3, 4))))
  }

  test("Read.CompositeOfInstances reads columns correctly") {
    import cats.syntax.functor.*
    val rscc: Read[SimpleCaseClass] = Read.derived[SimpleCaseClass]

    implicit val read: Read[ComplexCaseClass] =
      new Read.CompositeOfInstances(Array(
        rscc.widen[Any],
        rscc.toOpt.widen[Any],
        Read[Option[Int]].widen[Any],
        Read[String].widen[Any]))
        .map { arr =>
          ComplexCaseClass(
            arr(0).asInstanceOf[SimpleCaseClass],
            arr(1).asInstanceOf[Option[SimpleCaseClass]],
            arr(2).asInstanceOf[Option[Int]],
            arr(3).asInstanceOf[String])
        }

    for {
      _ <- IO(assertEquals(read.length, 8))
      _ <-
        sql"SELECT 1, '2', '3', 4, '5', '6', 7, '8'"
          .query[ComplexCaseClass].unique.transact(xa)
          .assertEquals(
            ComplexCaseClass(
              SimpleCaseClass(Some(1), "2", Some("3")),
              Some(SimpleCaseClass(Some(4), "5", Some("6"))),
              Some(7),
              "8"
            )
          )
      _ <-
        // The 's' field in Option[SimpleCaseClass] is NULL, so whole case class value is None
        sql"SELECT NULL, '2', '3', 4, NULL, '6', 7, '8'"
          .query[ComplexCaseClass].unique.transact(xa)
          .assertEquals(
            ComplexCaseClass(
              SimpleCaseClass(None, "2", Some("3")),
              None,
              Some(7),
              "8"
            )
          )
      _ <-
        sql"SELECT 1, NULL, '3', 4, '5', '6', 7, '8'"
          .query[Option[ComplexCaseClass]].unique.transact(xa)
          .assertEquals(None)
    } yield ()
  }

  test("Read should select correct columns when combined with `ap`") {
    import cats.syntax.all.*
    import doobie.implicits.*

    val r = Read[Int]
    val c = (r, r, r, r, r).tupled
    val q: ConnectionIO[List[(Int, Int, Int, Int, Int)]] = sql"SELECT 1, 2, 3, 4, 5".query(using c).to[List]
    q.transact(xa).assertEquals(List((1, 2, 3, 4, 5)))
  }

  test("Read should select correct columns when combined with `product`") {
    import cats.syntax.all.*
    import doobie.implicits.*

    val r = Read[Int].product(Read[Int].product(Read[Int]))
    val q: ConnectionIO[List[(Int, (Int, Int))]] = sql"SELECT 1, 2, 3".query(using r).to[List]
    q.transact(xa).assertEquals(List((1, (2, 3))))
  }

  test("Read typechecking should work for Tuples") {
    val frag = sql"SELECT 1, 's', 3.0 :: DOUBLE"

    assertSuccessTypecheckRead[(Int, String, Double)](frag) *>
      assertSuccessTypecheckRead[(Int, (String, Double))](frag) *>
      assertSuccessTypecheckRead[((Int, String), Double)](frag) *>
      assertSuccessTypecheckRead[(Int, Option[String], Double)](frag) *>
      assertSuccessTypecheckRead[(Option[Int], Option[(String, Double)])](frag) *>
      assertSuccessTypecheckRead[Option[((Int, String), Double)]](frag) *>
      assertWarnedTypecheckRead[(Boolean, String, Double)](frag) *>
      assertMisalignedTypecheckRead[(Int, String)](frag) *>
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
    ) *>
      assertSuccessTypecheckRead(
        sql"create table tab(c1 int, c2 varchar not null, c3 varchar)".update.run.flatMap(_ =>
          sql"SELECT c1,c2,c3 from tab".query[WrappedSimpleCaseClass].analysis)
      ) *>
      assertSuccessTypecheckRead(
        sql"create table tab(c1 int, c2 varchar, c3 varchar)".update.run.flatMap(_ =>
          sql"SELECT c1,c2,c3 from tab".query[Option[SimpleCaseClass]].analysis)
      ) *>
      assertSuccessTypecheckRead(
        sql"create table tab(c1 int, c2 varchar, c3 varchar)".update.run.flatMap(_ =>
          sql"SELECT c1,c2,c3 from tab".query[Option[WrappedSimpleCaseClass]].analysis)
      ) *>
      assertTypeErrorTypecheckRead(
        sql"create table tab(c1 binary, c2 varchar not null, c3 varchar)".update.run.flatMap(_ =>
          sql"SELECT c1,c2,c3 from tab".query[SimpleCaseClass].analysis)
      ) *>
      assertMisalignedNullabilityTypecheckRead(
        sql"create table tab(c1 int, c2 varchar, c3 varchar)".update.run.flatMap(_ =>
          sql"SELECT c1,c2,c3 from tab".query[SimpleCaseClass].analysis)
      ) *>
      assertSuccessTypecheckRead(
        sql"create table tab(c1 int, c2 varchar not null, c3 varchar, c4 int, c5 varchar, c6 varchar, c7 int, c8 varchar not null)"
          .update.run.flatMap(_ =>
            sql"SELECT c1,c2,c3,c4,c5,c6,c7,c8 from tab".query[ComplexCaseClass].analysis)
      ) *>
      assertTypeErrorTypecheckRead(
        sql"create table tab(c1 binary, c2 varchar not null, c3 varchar, c4 int, c5 varchar, c6 varchar, c7 int, c8 varchar not null)"
          .update.run.flatMap(_ =>
            sql"SELECT c1,c2,c3,c4,c5,c6,c7,c8 from tab".query[ComplexCaseClass].analysis)
      ) *>
      assertMisalignedNullabilityTypecheckRead(
        sql"create table tab(c1 int, c2 varchar, c3 varchar, c4 int, c5 varchar, c6 varchar, c7 int, c8 varchar not null)"
          .update.run.flatMap(_ =>
            sql"SELECT c1,c2,c3,c4,c5,c6,c7,c8 from tab".query[ComplexCaseClass].analysis)
      )
  }

  test("Derivation for big case class works") {
    Read.derived[Big30CaseClass].void

    import doobie.implicits.*
    Read.derived[Big30CaseClass].void
  }

  test("Derivation for big case class works") {
    Read.derived[Big30CaseClass].void

    import doobie.implicits.*
    Read[Big30CaseClass].void
  }

  private def insertTuple3AndCheckRead[Tup <: (?, ?, ?): Write, A: Read](in: Tup, expectedOut: A)(implicit
      loc: Location
  ): IO[Unit] =
    Query[Tup, A]("SELECT ?, ?, ?").unique(in).transact(xa).assertEquals(expectedOut)

  private def insertTuple2AndCheckRead[Tup <: (?, ?): Write, A: Read](in: Tup, expectedOut: A)(implicit
      loc: Location
  ): IO[Unit] =
    Query[Tup, A]("SELECT ?, ?").unique(in).transact(xa).assertEquals(expectedOut)

  private def assertSuccessTypecheckRead(connio: ConnectionIO[Analysis])(implicit loc: Location): IO[Unit] =
    connio.transact(xa).map(_.columnAlignmentErrors).assertEquals(Nil)

  private def assertSuccessTypecheckRead[A: Read](frag: Fragment)(implicit loc: Location): IO[Unit] =
    assertSuccessTypecheckRead(frag.query[A].analysis)

  private def assertWarnedTypecheckRead[A: Read](frag: Fragment)(implicit loc: Location): IO[Unit] =
    frag.query[A].analysis.transact(xa).map(_.columnAlignmentErrors.map(_.getClass)).assertEquals(List(
      classOf[ColumnTypeWarning]))

  private def assertTypeErrorTypecheckRead(connio: ConnectionIO[Analysis])(implicit loc: Location): IO[Unit] =
    connio.transact(xa).map(_.columnAlignmentErrors.map(_.getClass)).assertEquals(List(classOf[ColumnTypeError]))

  private def assertMisalignedNullabilityTypecheckRead(connio: ConnectionIO[Analysis])(implicit
      loc: Location
  ): IO[Unit] =
    connio.transact(xa).map(_.columnAlignmentErrors.map(_.getClass)).assertEquals(List(
      classOf[NullabilityMisalignment]))

  private def assertMisalignedTypecheckRead[A: Read](frag: Fragment)(implicit loc: Location): IO[Unit] =
    frag.query[A].analysis.transact(xa).map(_.columnAlignmentErrors.map(_.getClass)).assertEquals(List(
      classOf[ColumnMisalignment]))

}

object ReadSuite {
  case class Animal(id: Int, name: String)
  case class Person(id: Int, name: Option[String])
  case class PersonJustName(name: Option[String])
  case class PersonJustNameCustomOption(name: Option[String])

  object PersonJustNameCustomOption {
    implicit val read: Read[PersonJustNameCustomOption] = Read.derived[PersonJustNameCustomOption]
    implicit val readOption: Read[Option[PersonJustNameCustomOption]] =
      Read.optionalFromRead[PersonJustNameCustomOption].map {
        case Some(PersonJustNameCustomOption(None)) => None
        case other                                  => other
      }
  }
}
