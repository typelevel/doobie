// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import doobie.Transactor
import doobie.Update
import doobie.util.TestTypes.*
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.testutils.VoidExtensions

class WriteSuite extends munit.FunSuite with WriteSuitePlatform {

  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None
  )

  test("Write should exist for some fancy types") {
    import doobie.generic.auto.*

    Write[Int].void
    Write[(Int, Int)].void
    Write[(Int, Int, String)].void
    Write[(Int, (Int, String))].void
    Write[ComplexCaseClass].void
  }

  test("Write is auto derived for tuples without an import") {
    Write[(Int, Int)].void
    Write[(Int, Int, String)].void
    Write[(Int, (Int, String))].void

    Write[Option[(Int, Int)]].void
    Write[Option[(Int, Option[(String, Int)])]].void
  }

  test("Write is still auto derived for tuples when import is present (no ambiguous implicits) ") {
    import doobie.generic.auto.*
    Write[(Int, Int)].void
    Write[(Int, Int, String)].void
    Write[(Int, (Int, String))].void

    Write[Option[(Int, Int)]].void
    Write[Option[(Int, Option[(String, Int)])]].void
  }

  test("Write is not auto derived for case classes") {
    assert(compileErrors("Write[LenStr1]").contains("Cannot find or construct"))
  }

  test("Write should not be derivable for case objects") {
    assert(compileErrors("Write[CaseObj.type]").contains("Cannot find or construct"))
    assert(compileErrors("Write[Option[CaseObj.type]]").contains("Cannot find or construct"))
  }

  test("Write can be manually derived") {
    Write.derived[LenStr1].void
  }

  test("Write should exist for Unit") {
    import doobie.generic.auto.*

    Write[Unit].void
    assertEquals(Write[(Int, Unit)].length, 1)
  }

  test("Write should exist for option of some fancy types") {
    import doobie.generic.auto.*

    Write[Option[Int]].void
    Write[Option[(Int, Int)]].void
    Write[Option[(Int, Int, String)]].void
    Write[Option[(Int, (Int, String))]].void
    Write[Option[(Int, Option[(Int, String)])]].void
  }

  test("Write should exist for option of Unit") {
    import doobie.generic.auto.*

    Write[Option[Unit]].void
    assertEquals(Write[Option[(Int, Unit)]].length, 1)
  }

  test("Write should select multi-column instance by default") {
    import doobie.generic.auto.*

    assertEquals(Write[LenStr1].length, 2)
  }

  test("Write should select 1-column instance when available") {
    assertEquals(Write[LenStr2].length, 1)
  }

  test("Write should correct set parameters for Option instances ") {
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
    interceptMessage[RuntimeException]("Expected optional param but got Some(null).") {
      testNullPut(("a", Some(null)))
    }
  }

  test("Write should yield correct error when null inserted into non-nullable field") {
    interceptMessage[RuntimeException]("Expected non-nullable param. Use Option to describe nullable values.") {
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

}
