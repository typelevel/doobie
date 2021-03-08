// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.syntax.all._
import cats.effect.IO
import doobie._, doobie.implicits._
import doobie.util.log.{ LogEvent, Success, ProcessingFailure }

class LogSuite extends munit.FunSuite {

  import cats.effect.unsafe.implicits.global

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def eventForUniqueQuery[A: Write](sql: String, arg: A = ()): LogEvent = {
    var result  = null : LogEvent
    val handler = LogHandler(result = _)
    val cio     = Query[A, Unit](sql, None, handler).unique(arg)
    cio.transact(xa).attempt.unsafeRunSync()
    result
  }

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def eventForUniqueUpdate[A: Write](sql: String, arg: A = ()): LogEvent = {
    var result  = null : LogEvent
    implicit val handler = LogHandler(result = _)
    val cio     = sql"create table if not exists foo (bar integer)".update.run *>
                  Update[A](sql, None).run(arg)
    cio.transact(xa).attempt.unsafeRunSync()
    result
  }

  test("[Query] default handler") {
    sql"select 1".query[Int]
  }

  test("[Query] implicit handler") {
    var result  = null : LogEvent
    implicit val handler: LogHandler = LogHandler(result = _)
    val cio = sql"select 1".query[Int].unique
    cio.transact(xa).attempt.unsafeRunSync()
    result match {
      case Success(_, _, _, _) => ()
      case a => fail(s"no match: $a")
    }
  }

  test("[Query] explicit handler") {
    var result  = null : LogEvent
    val handler = LogHandler(result = _)
    val cio = sql"select 1".queryWithLogHandler[Int](handler).unique
    cio.transact(xa).attempt.unsafeRunSync()
    result match {
      case Success(_, _, _, _) => ()
      case a => fail(s"no match: $a")
    }
  }

  test("[Query] zero-arg success") {
    val Sql = "select 1"
    eventForUniqueQuery(Sql) match {
      case Success(Sql, Nil, _, _) => ()
      case a => fail(s"no match: $a")
    }
  }

  test("[Query] zero-arg execution failure".ignore) {
    ()
  }

  test("[Query] n-arg execution failure".ignore) {
    ()
  }

  test("[Query] zero-arg processing failure") {
    val Sql = "select 1 where 1 = 2"
    eventForUniqueQuery(Sql) match {
      case ProcessingFailure(Sql, Nil, _, _, _) => ()
      case a => fail(s"no match: $a")
    }
  }

  test("[Update] default handler") {
    sql"drop table if exists barf".update
  }

  test("[Update] implicit handler") {
    var result  = null : LogEvent
    implicit val handler: LogHandler = LogHandler(result = _)
    val cio = sql"drop table if exists barf".update.run
    cio.transact(xa).attempt.unsafeRunSync()
    result match {
      case Success(_, _, _, _) => ()
      case a => fail(s"no match: $a")
    }
  }

  test("[Update] explicit handler") {
    var result  = null : LogEvent
    val handler = LogHandler(result = _)
    val cio = sql"drop table if exists barf".updateWithLogHandler(handler).run
    cio.transact(xa).attempt.unsafeRunSync()
    result match {
      case Success(_, _, _, _) => ()
      case a => fail(s"no match: $a")
    }
  }

  test("[Update] zero-arg success") {
    val Sql = "update foo set bar = 42"
    eventForUniqueUpdate(Sql) match {
      case Success(Sql, Nil, _, _) => ()
      case a => fail(s"no match: $a")
    }
  }

  test("[Update] zero-arg execution failure".ignore) {
    ()
  }

  test("[Update] n-arg execution failure".ignore) {
    ()
  }

}
