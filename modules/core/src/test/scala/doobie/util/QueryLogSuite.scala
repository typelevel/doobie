// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.syntax.all.*
import cats.effect.{IO, Ref}
import doobie.free.connection.ConnectionIO
import doobie.implicits.*
import doobie.util.log.Parameters.NonBatch
import doobie.util.log.{ExecFailure, LogEvent, Parameters, ProcessingFailure, Success}
import doobie.util.query.Query
import doobie.util.transactor.Transactor
import doobie.util.update.Update

class QueryLogSuite extends munit.CatsEffectSuite with QueryLogSuitePlatform {

  val logEventRef: Ref[IO, LogEvent] =
    Ref.of[IO, LogEvent](null).unsafeRunSync()

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa",
    "",
    logHandler = Some(ev => logEventRef.set(ev))
  )

  def eventForCIO[A](cio: ConnectionIO[A]): IO[LogEvent] = {
    for {
      _ <- logEventRef.set(null)
      _ <- cio.transact(xa).attempt
      log <- logEventRef.get
    } yield log
  }

  def successEventForCIO[A](cio: ConnectionIO[A]): IO[Success] =
    eventForCIO(cio).map {
      case s: Success => s
      case other      => fail(s"Expected Success log event but got $other")
    }

  def execFailureEventForCIO[A](cio: ConnectionIO[A]): IO[ExecFailure] =
    eventForCIO(cio).map {
      case ev: ExecFailure => ev
      case other           => fail(s"Expected ExecFailure log event but got $other")
    }

  def processFailureEventForCIO[A](cio: ConnectionIO[A]): IO[ProcessingFailure] =
    eventForCIO(cio).map {
      case ev: ProcessingFailure => ev
      case other                 => fail(s"Expected ProcessingFailure log event but got $other")
    }

  def eventForUniqueQuery[A: Write](sql: String, arg: A): IO[LogEvent] = {
    eventForCIO(Query[A, Unit](sql, None).unique(arg))
  }

  def eventForUpdate[A: Write](sql: String, arg: A): IO[LogEvent] = {
    val cio = sql"create table if not exists foo (bar integer)".update.run *>
      Update[A](sql, None).run(arg)
    eventForCIO(cio)
  }

  test("simple") {
    val q: query.Query0[(Int, Int)] = sql"select 1, 2".query[(Int, Int)]
    List(
      successEventForCIO(q.to[List]),
      successEventForCIO(q.toMap[Int, Int]),
      successEventForCIO(q.accumulate[List]),
      successEventForCIO(q.unique),
      successEventForCIO(q.option),
      successEventForCIO(q.nel)
    )
      .sequence
      .map { succEvents =>
        succEvents.foreach { succ =>
          assertEquals(succ.sql, "select 1, 2")
          assertEquals(succ.params, NonBatch(Nil))
          assertEquals(succ.label, "unlabeled")
        }
      }

  }

  test("With params and label") {
    val q = sql"select ${1}, ${"2"}".queryWithLabel[(Int, String)]("mylabel")
    List(
      successEventForCIO(q.to[List]),
      successEventForCIO(q.toMap[Int, String]),
      successEventForCIO(q.accumulate[List]),
      successEventForCIO(q.unique),
      successEventForCIO(q.option),
      successEventForCIO(q.nel)
    )
      .sequence
      .map { succEvents =>
        succEvents.foreach { succ =>
          assertEquals(succ.sql, "select ?, ?")
          assertEquals(succ.params, NonBatch(List(1, "2")))
          assertEquals(succ.label, "mylabel")
          assert(succ.exec.toNanos > 0L)
          assert(succ.processing.toNanos > 0L)
        }
      }
  }

  test("execution failure (Error during PreparedStatement construction)") {
    val q = sql"select bad_column".query[(Int, String)]
    List(
      execFailureEventForCIO(q.to[List]),
      execFailureEventForCIO(q.toMap[Int, String]),
      execFailureEventForCIO(q.accumulate[List]),
      execFailureEventForCIO(q.unique),
      execFailureEventForCIO(q.option),
      execFailureEventForCIO(q.nel)
    )
      .sequence
      .map { failEvents =>
        failEvents.foreach { ev =>
          assertEquals(ev.sql, "select bad_column")
          assertEquals(ev.params, Parameters.nonBatchEmpty)
          assertEquals(ev.label, "unlabeled")
          assertEquals(ev.exec.toNanos, 0L)
          assert(ev.failure.getMessage.contains("not found"))
        }
      }
  }

  test("execution failure") {
    val q = Query[String, (Int, String)]("select ? :: Int")
    List(
      execFailureEventForCIO(q.to[List]("not_int")),
      execFailureEventForCIO(q.toMap[Int, String]("not_int")),
      execFailureEventForCIO(q.accumulate[List]("not_int")),
      execFailureEventForCIO(q.unique("not_int")),
      execFailureEventForCIO(q.option("not_int")),
      execFailureEventForCIO(q.nel("not_int"))
    )
      .sequence
      .map { failEvents =>
        failEvents.foreach { ev =>
          assertEquals(ev.sql, "select ? :: Int")
          assertEquals(ev.params, NonBatch(List("not_int")))
          assertEquals(ev.label, "unlabeled")
          assert(ev.exec.toNanos > 0L)
          assert(ev.failure.getMessage.contains("Data conversion error"))
        }
      }
  }
  test("processing failure") {
    val q = sql"select 'not_int'".query[(Int, String)]
    List(
      processFailureEventForCIO(q.to[List]),
      processFailureEventForCIO(q.toMap[Int, String]),
      processFailureEventForCIO(q.accumulate[List]),
      processFailureEventForCIO(q.unique),
      processFailureEventForCIO(q.option),
      processFailureEventForCIO(q.nel)
    )
      .sequence
      .map { failEvents =>
        failEvents.foreach { ev =>
          assertEquals(ev.sql, "select 'not_int'")
          assertEquals(ev.params, Parameters.nonBatchEmpty)
          assertEquals(ev.label, "unlabeled")
          assert(ev.exec.toNanos > 0L)
          assert(ev.failure.getMessage.contains("Data conversion error"))
        }
      }
  }
  test("stream") {
    val sql = "select * from values (1),(2),(3),(4),(5),(6),(7),(8),(9),(10)"
    successEventForCIO(
      Query[Unit, Int](sql).stream(()).compile.toList
    ).map { ev =>
      assertEquals(ev.sql, sql)
      assertEquals(ev.params, NonBatch(Nil))
      assertEquals(ev.label, "unlabeled")
      assert(ev.exec.toNanos > 0L)
      assertEquals(ev.processing.toNanos, 0L)
    }
  }

  test("streamWithChunkSize") {
    val sql = "select * from values (1),(2),(3),(4),(5),(6),(7),(8),(9),(10)"
    successEventForCIO(
      Query[Unit, Int](sql).streamWithChunkSize((), 5).compile.toList
    ).map { ev =>
      assertEquals(ev.sql, sql)
      assertEquals(ev.params, NonBatch(Nil))
      assertEquals(ev.label, "unlabeled")
      assert(ev.exec.toNanos > 0L)
      assertEquals(ev.processing.toNanos, 0L)
    }
  }

  test("stream: Log ExecFailure on failed PreparedStatement construction") {
    val q0 = sql"select bad_column".query[Int]
    List(
      execFailureEventForCIO(q0.stream.compile.toList),
      execFailureEventForCIO(q0.streamWithChunkSize(1).compile.toList)
    ).sequence
      .map { failEvents =>
        failEvents.foreach { ev =>
          assertEquals(ev.sql, "select bad_column")
          assertEquals(ev.params, Parameters.nonBatchEmpty)
          assertEquals(ev.label, "unlabeled")
          assertEquals(ev.exec.toNanos, 0L)
          assert(ev.failure.getMessage.contains("not found"))
        }
      }
  }

  test("stream: Log ExecFailure on failed PreparedStatement execution") {
    val q0 = Query[String, (Int, String)]("select ? :: Int")
    List(
      execFailureEventForCIO(q0.stream("not_int").compile.toList),
      execFailureEventForCIO(q0.streamWithChunkSize("not_int", 1).compile.toList)
    ).sequence
      .map { failEvents =>
        failEvents.foreach { ev =>
          assertEquals(ev.sql, "select ? :: Int")
          assertEquals(ev.params, NonBatch(List("not_int")))
          assertEquals(ev.label, "unlabeled")
          assert(ev.exec.toNanos > 0L)
          assert(ev.failure.getMessage.contains("Data conversion error"))
        }
      }
  }

}
