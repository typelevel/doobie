// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.syntax.all.*
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.{Transactor, Update}
import doobie.free.preparedstatement as IFPS

class UpdateSuite extends munit.FunSuite {
  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None
  )

  test("Update runAlteringExecution") {
    import doobie.implicits.*
    var didRun = false
    (for {
      _ <- sql"create temp table t1 (a int)".update.run
      res <- Update[Int]("insert into t1 (a) values (?)").runAlteringExecution(
        1,
        pe => pe.copy(exec = IFPS.delay { didRun = true } *> pe.exec))
    } yield {
      assertEquals(res, 1)
    })
      .transact(xa)
      .unsafeRunSync()

    assert(didRun)
  }

  test("Update updateManyAlteringExecution") {
    import doobie.implicits.*
    var didRun = false
    (for {
      _ <- sql"create temp table t1 (a int)".update.run
      res <- Update[Int]("insert into t1 (a) values (?)").updateManyAlteringExecution(
        List(2, 4, 6, 8),
        pe => pe.copy(exec = IFPS.delay { didRun = true } *> pe.exec))
    } yield {
      assertEquals(res, 4)
    })
      .transact(xa)
      .unsafeRunSync()

    assert(didRun)
  }

  test("Update withUniqueGeneratedKeysAlteringExecution") {
    import doobie.implicits.*
    var didRun = false
    (for {
      _ <- sql"create temp table t1 (a int, b int)".update.run
      res <- Update[(Int, Int)]("insert into t1 (a, b) values (?, ?)")
        .withUniqueGeneratedKeysAlteringExecution[(Int, Int)]("a", "b")(
          (5, 6),
          pe => pe.copy(exec = IFPS.delay { didRun = true } *> pe.exec)
        )
    } yield {
      assertEquals(res, (5, 6))
    })
      .transact(xa)
      .unsafeRunSync()

    assert(didRun)
  }

}
