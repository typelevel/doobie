// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.{IO, IOLocal}
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.util.log.Parameters.NonBatch
import doobie.util.log.*

import scala.annotation.nowarn

@nowarn("msg=.*inferred to be `Any`.*")
class UpdateLogSuite extends munit.FunSuite {

  import cats.effect.unsafe.implicits.global

  val ioLocal: IOLocal[LogEvent] =
    IOLocal[LogEvent](null).unsafeRunSync()

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa",
    "",
    logHandler = Some(ev => ioLocal.set(ev))
  )

  def eventForCIO[A](cio: ConnectionIO[A]): LogEvent =
    (
      sql"create table if not exists foo (c1 integer, c2 varchar)".update.run *> cio
    )
      .transact(xa)
      .attempt
      .flatMap { res =>
        val _ = res
        ioLocal.get
      }
      .unsafeRunSync()

  def successEventForCIO[A](cio: ConnectionIO[A]): Success =
    eventForCIO(cio) match {
      case s: Success => s
      case other      => fail(s"Expected Success log event but got $other")
    }

  def execFailureEventForCIO[A](cio: ConnectionIO[A]): ExecFailure =
    eventForCIO(cio) match {
      case ev: ExecFailure => ev
      case other           => fail(s"Expected ExecFailure log event but got $other")
    }

  def processFailureEventForCIO[A](cio: ConnectionIO[A]): ProcessingFailure =
    eventForCIO(cio) match {
      case ev: ProcessingFailure => ev
      case other                 => fail(s"Expected ProcessingFailure log event but got $other")
    }

  test("update") {
    val cio = sql"INSERT INTO foo VALUES (${1}, ${"str"})".update.run
    val ev = successEventForCIO(cio)
    assertEquals(ev.sql, "INSERT INTO foo VALUES (?, ?)")
    assertEquals(ev.params, NonBatch(List(1, "str")))
    assertEquals(ev.label, "unlabeled")
  }

  test("update: Log ExecFailure on failed PreparedStatement construction") {
    val ev = execFailureEventForCIO(sql"insert into hm".update.run)

    assertEquals(ev.sql, "insert into hm")
    assertEquals(ev.params, Parameters.nonBatchEmpty)
    assertEquals(ev.label, "unlabeled")
    assertEquals(ev.exec.toNanos, 0L)
    assert(ev.failure.getMessage.contains("""Table "HM" not found"""))
  }

  test("update: Log ExecFailure on failed PreparedStatement execution") {
    val cio = Update[(String, String)]("insert into foo values (?, ?)")
      .run(("s", "k"))
    val ev = execFailureEventForCIO(cio)

    assertEquals(ev.sql, "insert into foo values (?, ?)")
    assertEquals(ev.params, NonBatch(List("s", "k")))
    assertEquals(ev.label, "unlabeled")
    assert(ev.exec.toNanos > 0L)
    assert(ev.failure.getMessage.contains("Data conversion error"))
  }

  test("updateMany") {
    val cio = Update[(Int, Int)]("INSERT INTO foo VALUES (?, ?)", label = "some_label")
      .updateMany(List((1, 2), (3, 4), (5, 6)))
    val ev = successEventForCIO(cio)
    assertEquals(ev.sql, "INSERT INTO foo VALUES (?, ?)")
    ev.params match {
      case NonBatch(_) => fail("Expect batched params")
      case Parameters.Batch(paramsAsLists) =>
        assertEquals(paramsAsLists(), List(List(1, 2), List(3, 4), List(5, 6)))
    }
    assertEquals(ev.label, "some_label")
    assert(ev.exec.toNanos > 0L)
  }

  test("updateMany: Log ExecFailure on failed PreparedStatement construction") {
    val ev = execFailureEventForCIO(Update[Int]("insert into hm").updateMany(List(1, 2)))
    assertEquals(ev.sql, "insert into hm")
    assertEquals(ev.params.allParams, List(List(1), List(2)))
    assertEquals(ev.label, "unlabeled")
    assertEquals(ev.exec.toNanos, 0L)
    assert(ev.failure.getMessage.contains("""Table "HM" not found"""))
  }

  test("updateMany Log ExecFailure on failed PreparedStatement execution") {
    val cio = Update[(String, String)]("insert into foo values (?, ?)")
      .updateMany(List(("s", "k")))
    val ev = execFailureEventForCIO(cio)

    assertEquals(ev.sql, "insert into foo values (?, ?)")
    assertEquals(ev.params.allParams, List(List("s", "k")))
    assertEquals(ev.label, "unlabeled")
    assert(ev.exec.toNanos > 0L)
  }

  test("updateManyWithGeneratedKeys") {
    val cio = Update[(Int, String)]("insert into foo values (?, ?)")
      .updateManyWithGeneratedKeys[String]("c1")(List((1, "a"), (2, "b")))
      .compile
      .toList

    val ev = successEventForCIO(cio)

    assertEquals(ev.sql, "insert into foo values (?, ?)")
    assertEquals(ev.params.allParams, List(List(1, "a"), List(2, "b")))
    assertEquals(ev.label, "unlabeled")
    assert(ev.exec.toNanos > 0L)
    assertEquals(ev.processing.toNanos, 0L)
  }

  test("updateManyWithGeneratedKeys: Log ExecFailure on failed PreparedStatement construction") {
    val cio = Update[(Int, String)]("insert into foo")
      .updateManyWithGeneratedKeys[String]("c1")(List((1, "a"), (2, "b")))
      .compile
      .toList

    val ev = execFailureEventForCIO(cio)

    assertEquals(ev.sql, "insert into foo")
    assertEquals(ev.params.allParams, List(List(1, "a"), List(2, "b")))
    assertEquals(ev.label, "unlabeled")
    assertEquals(ev.exec.toNanos, 0L)
    assert(ev.failure.getMessage.contains("Syntax error"))
  }

  test("updateManyWithGeneratedKeys: Log ExecFailure on failed PreparedStatement execution") {
    val cio = Update[(String, String)]("insert into foo values (?, ?)")
      .updateManyWithGeneratedKeys[String]("c1")(List(("s", "k")))
      .compile
      .toList
    val ev = execFailureEventForCIO(cio)

    assertEquals(ev.sql, "insert into foo values (?, ?)")
    assertEquals(ev.params.allParams, List(List("s", "k")))
    assertEquals(ev.label, "unlabeled")
    assert(ev.exec.toNanos > 0L)
    assert(ev.failure.getMessage.contains("Data conversion error"))
  }

  test("withGeneratedKeys") {
    val cio = Update[(Int, String)]("insert into foo values (?, ?)")
      .withGeneratedKeys[String]("c1")((1, "a"))
      .compile
      .toList

    val ev = successEventForCIO(cio)

    assertEquals(ev.sql, "insert into foo values (?, ?)")
    assertEquals(ev.params, NonBatch(List(1, "a")))
    assertEquals(ev.label, "unlabeled")
    assert(ev.exec.toNanos > 0L)
    assertEquals(ev.processing.toNanos, 0L)
  }

  test("withGeneratedKeys: Log ExecFailure on failed PreparedStatement construction") {
    val cio = Update[(Int, String)]("insert into foo")
      .withGeneratedKeys[String]("c1")((1, "a"))
      .compile
      .toList

    val ev = execFailureEventForCIO(cio)

    assertEquals(ev.sql, "insert into foo")
    assertEquals(ev.params, NonBatch(List(1, "a")))
    assertEquals(ev.label, "unlabeled")
    assertEquals(ev.exec.toNanos, 0L)
    assert(ev.failure.getMessage.contains("Syntax error"))
  }

  test("withGeneratedKeys: Log ExecFailure on failed PreparedStatement execution") {
    val cio = Update[(String, String)]("insert into foo values (?, ?)")
      .withGeneratedKeys[String]("c1")(("s", "k"))
      .compile
      .toList
    val ev = execFailureEventForCIO(cio)

    assertEquals(ev.sql, "insert into foo values (?, ?)")
    assertEquals(ev.params, NonBatch(List("s", "k")))
    assertEquals(ev.label, "unlabeled")
    assert(ev.exec.toNanos > 0L)
    assert(ev.failure.getMessage.contains("Data conversion error"))
  }

  test("withUniqueGeneratedKeys") {
    val cio = Update[(Int, String)]("insert into foo values (?, ?)")
      .withUniqueGeneratedKeys[String]("c1")((1, "a"))

    val ev = successEventForCIO(cio)

    assertEquals(ev.sql, "insert into foo values (?, ?)")
    assertEquals(ev.params, NonBatch(List(1, "a")))
    assertEquals(ev.label, "unlabeled")
    assert(ev.exec.toNanos > 0L)
    assert(ev.processing.toNanos > 0L)
  }

  test("withUniqueGeneratedKeys: Log ExecFailure on failed PreparedStatement construction") {
    val cio = Update[(Int, String)]("insert into foo")
      .withUniqueGeneratedKeys[String]("c1")((1, "a"))

    val ev = execFailureEventForCIO(cio)

    assertEquals(ev.sql, "insert into foo")
    assertEquals(ev.params, NonBatch(List(1, "a")))
    assertEquals(ev.label, "unlabeled")
    assertEquals(ev.exec.toNanos, 0L)
    assert(ev.failure.getMessage.contains("Syntax error"))
  }

  test("withUniqueGeneratedKeys: Log ExecFailure on failed PreparedStatement execution") {
    val cio = Update[(String, String)]("insert into foo values (?, ?)")
      .withUniqueGeneratedKeys[String]("c1")(("s", "k"))
    val ev = execFailureEventForCIO(cio)

    assertEquals(ev.sql, "insert into foo values (?, ?)")
    assertEquals(ev.params, NonBatch(List("s", "k")))
    assertEquals(ev.label, "unlabeled")
    assert(ev.exec.toNanos > 0L)
    assert(ev.failure.getMessage.contains("Data conversion error"))
  }

}
