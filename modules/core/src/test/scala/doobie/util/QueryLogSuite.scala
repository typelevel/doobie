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

  val logEventRef: IO[Ref[IO, LogEvent]] =
    Ref.of[IO, LogEvent](null)

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa",
    "",
    logHandler = Some(ev => logEventRef.unsafeRunSync().set(ev))
  )

  def eventForCIO[A](cio: ConnectionIO[A]): IO[LogEvent] = {
    for {
      _ <- logEventRef.flatMap(_.set(null))
      _ <- cio.transact(xa).attempt
      log <- logEventRef.flatMap(_.get)
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
    val succEvents: List[IO[Success]] = List(
      successEventForCIO(q.to[List]),
      successEventForCIO(q.toMap[Int, Int]),
      successEventForCIO(q.accumulate[List]),
      successEventForCIO(q.unique),
      successEventForCIO(q.option),
      successEventForCIO(q.nel)
    )

    succEvents.map { succ =>
      assertIO(succ.map(_.sql), "select 1, 2") *>
        assertIO(succ.map(_.params), NonBatch(Nil)) *>
        assertIO(succ.map(_.label), "unlabeled")
    }.sequence_
  }

  test("With params and label") {
    val q = sql"select ${1}, ${"2"}".queryWithLabel[(Int, String)]("mylabel")
    val succEvents = List(
      successEventForCIO(q.to[List]),
      successEventForCIO(q.toMap[Int, String]),
      successEventForCIO(q.accumulate[List]),
      successEventForCIO(q.unique),
      successEventForCIO(q.option),
      successEventForCIO(q.nel)
    )
    succEvents.map { succ =>
      assertIO(succ.map(_.sql), "select ?, ?") *>
        assertIO(succ.map(_.params), NonBatch(List(1, "2"))) *>
        assertIO(succ.map(_.label), "mylabel") *>
        assertIOBoolean(succ.map(_.exec.toNanos > 0L)) *>
        assertIOBoolean(succ.map(_.processing.toNanos > 0L))
    }.sequence_
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
    ).map { ev =>
      assertIO(ev.map(_.sql), "select bad_column") *>
        assertIO(ev.map(_.params), Parameters.nonBatchEmpty) *>
        assertIO(ev.map(_.label), "unlabeled") *>
        assertIO(ev.map(_.exec.toNanos), 0L) *>
        assertIOBoolean(ev.map(_.failure.getMessage.contains("not found")))
    }.sequence
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
    ).map { ev =>
      assertIO(ev.map(_.sql), "select ? :: Int") *>
        assertIO(ev.map(_.params), NonBatch(List("not_int"))) *>
        assertIO(ev.map(_.label), "unlabeled") *>
        assertIOBoolean(ev.map(_.exec.toNanos > 0L)) *>
        assertIOBoolean(ev.map(_.failure.getMessage.contains("Data conversion error")))
    }.sequence
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
    ).map { ev =>
      assertIO(ev.map(_.sql), "select 'not_int'") *>
        assertIO(ev.map(_.params), Parameters.nonBatchEmpty) *>
        assertIO(ev.map(_.label), "unlabeled") *>
        assertIOBoolean(ev.map(_.exec.toNanos > 0L)) *>
        assertIOBoolean(ev.map(_.failure.getMessage.contains("Data conversion error")))
    }.sequence_
  }

  test("stream") {
    val sql = "select * from values (1),(2),(3),(4),(5),(6),(7),(8),(9),(10)"
    val succ = successEventForCIO(
      Query[Unit, Int](sql).stream(()).compile.toList
    )
    assertIO(succ.map(_.sql), sql) *>
      assertIO(succ.map(_.params), NonBatch(Nil)) *>
      assertIO(succ.map(_.label), "unlabeled") *>
      assertIOBoolean(succ.map(_.exec.toNanos > 0L)) *>
      assertIO(succ.map(_.processing.toNanos), 0L)
  }

  test("streamWithChunkSize") {
    val sql = "select * from values (1),(2),(3),(4),(5),(6),(7),(8),(9),(10)"
    val succ = successEventForCIO(
      Query[Unit, Int](sql).streamWithChunkSize((), 5).compile.toList
    )
    assertIO(succ.map(_.sql), sql) *>
      assertIO(succ.map(_.params), NonBatch(Nil)) *>
      assertIO(succ.map(_.label), "unlabeled") *>
      assertIOBoolean(succ.map(_.exec.toNanos > 0L)) *>
      assertIO(succ.map(_.processing.toNanos), 0L)
  }

  test("stream: Log ExecFailure on failed PreparedStatement construction") {
    val q0 = sql"select bad_column".query[Int]
    List(
      execFailureEventForCIO(q0.stream.compile.toList),
      execFailureEventForCIO(q0.streamWithChunkSize(1).compile.toList)
    ).map { ev =>
      assertIO(ev.map(_.sql), "select bad_column") *>
        assertIO(ev.map(_.params), Parameters.nonBatchEmpty) *>
        assertIO(ev.map(_.label), "unlabeled") *>
        assertIO(ev.map(_.exec.toNanos), 0L) *>
        assertIOBoolean(ev.map(_.failure.getMessage.contains("not found")))
    }.sequence_
  }

  test("stream: Log ExecFailure on failed PreparedStatement execution") {
    val q0 = Query[String, (Int, String)]("select ? :: Int")
    List(
      execFailureEventForCIO(q0.stream("not_int").compile.toList),
      execFailureEventForCIO(q0.streamWithChunkSize("not_int", 1).compile.toList)
    ).map { ev =>
      assertIO(ev.map(_.sql), "select ? :: Int") *>
        assertIO(ev.map(_.params), NonBatch(List("not_int"))) *>
        assertIO(ev.map(_.label), "unlabeled") *>
        assertIOBoolean(ev.map(_.exec.toNanos > 0L)) *>
        assertIOBoolean(ev.map(_.failure.getMessage.contains("Data conversion error")))
    }.sequence_
  }

}
