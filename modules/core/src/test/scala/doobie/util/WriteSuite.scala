// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import doobie.{Query, Transactor, Update}
import doobie.util.TestTypes.*
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.testutils.VoidExtensions
import doobie.syntax.all.*
import munit.Location
import scala.annotation.nowarn

class WriteSuite extends munit.FunSuite with WriteSuitePlatform {

  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None
  )

  test("Write is available for tuples without an import when all elements have a Write instance") {
    Write[(Int, Int)].void
    Write[(Int, Int, String)].void
    Write[(Int, (Int, String))].void

    Write[Option[(Int, Int)]].void
    Write[Option[(Int, Option[(String, Int)])]].void

    // But shouldn't automatically derive anything that doesn't already have a Read instance
    assert(compileErrors("Write[(Int, TrivialCaseClass)]").contains("Cannot find or construct"))
  }

  test("Write is still auto derived for tuples when import is present (no ambiguous implicits) ") {
    import doobie.implicits.*
    Write[(Int, Int)].void
    Write[(Int, Int, String)].void
    Write[(Int, (Int, String))].void

    Write[Option[(Int, Int)]].void
    Write[Option[(Int, Option[(String, Int)])]].void

    Write[(ComplexCaseClass, Int)].void
    Write[(Int, ComplexCaseClass)].void
  }

  test("Write is not auto derived for case classes") {
    assert(compileErrors("Write[TrivialCaseClass]").contains("Cannot find or construct"))
  }

  test("Semiauto derivation selects custom Write instances when available") {
    implicit val i0: Write[HasCustomReadWrite0] = Write.derived[HasCustomReadWrite0]
    assertEquals(i0.length, 2)
    writeAndCheckTuple(HasCustomReadWrite0(CustomReadWrite("x"), "y"), ("x_W", "y"))

    implicit val i1: Write[HasCustomReadWrite1] = Write.derived[HasCustomReadWrite1]
    assertEquals(i1.length, 2)
    writeAndCheckTuple(HasCustomReadWrite1("x", CustomReadWrite("y")), ("x", "y_W"))

    implicit val iOpt0: Write[HasOptCustomReadWrite0] = Write.derived[HasOptCustomReadWrite0]
    assertEquals(iOpt0.length, 2)
    writeAndCheckTuple(HasOptCustomReadWrite0(Some(CustomReadWrite("x")), "y"), ("x_W", "y"))

    implicit val iOpt1: Write[HasOptCustomReadWrite1] = Write.derived[HasOptCustomReadWrite1]
    assertEquals(iOpt1.length, 2)
    writeAndCheckTuple(HasOptCustomReadWrite1("x", Some(CustomReadWrite("y"))), ("x", "y_W"))
  }

  test("Semiauto derivation selects custom Put instances to use for Write when available") {
    implicit val i0: Write[HasCustomGetPut0] = Write.derived[HasCustomGetPut0]
    assertEquals(i0.length, 2)
    writeAndCheckTuple(HasCustomGetPut0(CustomGetPut("x"), "y"), ("x_P", "y"))

    implicit val i1: Write[HasCustomGetPut1] = Write.derived[HasCustomGetPut1]
    assertEquals(i1.length, 2)
    writeAndCheckTuple(HasCustomGetPut1("x", CustomGetPut("y")), ("x", "y_P"))

    implicit val iOpt0: Write[HasOptCustomGetPut0] = Write.derived[HasOptCustomGetPut0]
    assertEquals(iOpt0.length, 2)
    writeAndCheckTuple(HasOptCustomGetPut0(Some(CustomGetPut("x")), "y"), ("x_P", "y"))

    implicit val iOpt1: Write[HasOptCustomGetPut1] = Write.derived[HasOptCustomGetPut1]
    assertEquals(iOpt1.length, 2)
    writeAndCheckTuple(HasOptCustomGetPut1("x", Some(CustomGetPut("y"))), ("x", "y_P"))
  }

  test("Automatic derivation selects custom Write instances when available") {
    import doobie.implicits.*

    writeAndCheckTuple(HasCustomReadWrite0(CustomReadWrite("x"), "y"), ("x_W", "y"))
    writeAndCheckTuple(HasCustomReadWrite1("x", CustomReadWrite("y")), ("x", "y_W"))
    writeAndCheckTuple(HasOptCustomReadWrite0(Some(CustomReadWrite("x")), "y"), ("x_W", "y"))
    writeAndCheckTuple(HasOptCustomReadWrite1("x", Some(CustomReadWrite("y"))), ("x", "y_W"))
  }

  test("Automatic derivation selects custom Put instances to use for Write when available") {
    import doobie.implicits.*
    writeAndCheckTuple(HasCustomGetPut0(CustomGetPut("x"), "y"), ("x_P", "y"))
    writeAndCheckTuple(HasCustomGetPut1("x", CustomGetPut("y")), ("x", "y_P"))
    writeAndCheckTuple(HasOptCustomGetPut0(Some(CustomGetPut("x")), "y"), ("x_P", "y"))
    writeAndCheckTuple(HasOptCustomGetPut1("x", Some(CustomGetPut("y"))), ("x", "y_P"))
  }

  test("Write should not be derivable for case objects") {
    val expectedDeriveError =
      if (util.Properties.versionString.startsWith("version 2.12"))
        "could not find implicit"
      else
        "Cannot derive"
    assert(compileErrors("Write.derived[CaseObj.type]").contains(expectedDeriveError))
    assert(compileErrors("Write.derived[Option[CaseObj.type]]").contains(expectedDeriveError))

    import doobie.implicits.*
    assert(compileErrors("Write[Option[CaseObj.type]]").contains("not find or construct"))
    assert(compileErrors("Write[CaseObj.type]").contains("not find or construct"))
  }: @nowarn("msg=.*(u|U)nused import.*")

  test("Write should exist for Unit/Option[Unit]") {
    assertEquals(Write[Unit].length, 0)
    assertEquals(Write[Option[Unit]].length, 0)
    assertEquals(Write[(Int, Unit)].length, 1)
  }

  test("Write should correctly set parameters for Option instances ") {
    import doobie.implicits.*
    (for {
      _ <- sql"create temp table t1 (a int, b int)".update.run
      _ <- Update[Option[(Int, Int)]]("insert into t1 (a, b) values (?, ?)").run(Some((1, 2)))
      _ <- Update[Option[(Option[Int], Int)]]("insert into t1 (a, b) values (?, ?)").run(Some((None, 4)))
      _ <- Update[Option[(Int, Option[Int])]]("insert into t1 (a, b) values (?, ?)").run(Some((5, None)))
      _ <- Update[Option[(Option[Int], Int)]]("insert into t1 (a, b) values (?, ?)").run(None)
      _ <- Update[Option[(Int, Option[Int])]]("insert into t1 (a, b) values (?, ?)").run(None)
      res <- sql"select a, b from t1 order by a asc nulls last".query[(Option[Int], Option[Int])].to[List]
    } yield {
      assertEquals(
        res,
        List(
          (Some(1), Some(2)),
          (Some(5), None),
          (None, Some(4)),
          (None, None),
          (None, None)
        ))
    })
      .transact(xa)
      .unsafeRunSync()
  }

  test("Write should yield correct error when Some(null) inserted") {
    interceptMessage[RuntimeException]("Expected non-nullable param at 2. Use Option to describe nullable values.") {
      testNullPut(("a", Some(null)))
    }
  }

  test("Write should yield correct error when null inserted into non-nullable field") {
    interceptMessage[RuntimeException]("Expected non-nullable param at 1. Use Option to describe nullable values.") {
      testNullPut((null, Some("b")))
    }
  }

  private def testNullPut(input: (String, Option[String])): Int = {
    import doobie.implicits.*

    (for {
      _ <- sql"create temp table t0 (a text, b text null)".update.run
      n <- Update[(String, Option[String])]("insert into t0 (a, b) values (?, ?)").run(input)
    } yield n)
      .transact(xa)
      .unsafeRunSync()
  }

  private def writeAndCheckTuple[A: Write, Tup: Read](in: A, expectedOut: Tup)(implicit loc: Location): Unit = {
    val res = Query[A, Tup]("SELECT ?, ?").unique(in).transact(xa)
      .unsafeRunSync()
    assertEquals(res, expectedOut)
  }

}

object WriteSuite {}
