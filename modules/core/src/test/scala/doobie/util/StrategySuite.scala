// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.data.Kleisli
import cats.effect.IO
import cats.syntax.apply.*
import cats.~>
import doobie.*
import doobie.free.{connection, preparedstatement, resultset}
import doobie.implicits.*
import doobie.util.transactor.Transactor.Aux

import java.sql.{Connection, PreparedStatement, ResultSet}
import java.util

class StrategySuite extends munit.FunSuite {

  import cats.effect.unsafe.implicits.global

  val baseXa: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None
  )

  // an instrumented interpreter
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  class Interp extends KleisliInterpreter[IO](LogHandler.noop) {

    object Connection {
      var autoCommit: Option[Boolean] = None
      var close: Option[Unit] = None
      var commit: Option[Unit] = None
      var rollback: Option[Unit] = None
    }

    object PreparedStatement {
      var close: Option[Unit] = None
    }

    object ResultSet {
      var close: Option[Unit] = None
    }

    override lazy val ConnectionInterpreter: connection.ConnectionOp ~> Kleisli[IO, Connection, *] =
      new ConnectionInterpreter {
        override val close: Kleisli[IO, Connection, Unit] = delay(Connection.close = Some(())) *> super.close
        override val rollback: Kleisli[IO, Connection, Unit] = delay(Connection.rollback = Some(())) *> super.rollback
        override val commit: Kleisli[IO, Connection, Unit] = delay(Connection.commit = Some(())) *> super.commit
        override def setAutoCommit(b: Boolean): Kleisli[IO, Connection, Unit] =
          delay(Connection.autoCommit = Option(b)) *> super.setAutoCommit(b)
        override def getTypeMap: Kleisli[IO, Connection, util.Map[String, Class[?]]] =
          super.getTypeMap.asInstanceOf // No idea. Type error on Java 8 if we don't do this.
      }

    override lazy val PreparedStatementInterpreter
        : preparedstatement.PreparedStatementOp ~> Kleisli[IO, PreparedStatement, *] =
      new PreparedStatementInterpreter {
        override val close: Kleisli[IO, PreparedStatement, Unit] =
          delay(PreparedStatement.close = Some(())) *> super.close
      }

    override lazy val ResultSetInterpreter: resultset.ResultSetOp ~> Kleisli[IO, ResultSet, *] =
      new ResultSetInterpreter {
        override val close: Kleisli[IO, ResultSet, Unit] = delay(ResultSet.close = Some(())) *> super.close
      }

  }

  def xa(i: KleisliInterpreter[IO]): Transactor[IO] =
    Transactor.interpret.set(baseXa, i.ConnectionInterpreter)

  test("Connection.autoCommit should be set to false") {
    val i = new Interp
    val _ = sql"select 1".query[Int].unique.transact(xa(i)).unsafeRunSync()
    assertEquals(i.Connection.autoCommit, Some(false))
  }

  test("Connection.commit should be called on success") {
    val i = new Interp
    val _ = sql"select 1".query[Int].unique.transact(xa(i)).unsafeRunSync()
    assertEquals(i.Connection.commit, Some(()))
  }

  test("Connection.commit should NOT be called on failure") {
    val i = new Interp
    assertEquals(sql"abc".query[Int].unique.transact(xa(i)).attempt.unsafeRunSync().toOption, None)
    assertEquals(i.Connection.commit, None)
  }

  test("Connection.rollback should NOT be called on success") {
    val i = new Interp
    val _ = sql"select 1".query[Int].unique.transact(xa(i)).unsafeRunSync()
    assertEquals(i.Connection.rollback, None)
  }

  test("Connection.rollback should be called on failure") {
    val i = new Interp
    assertEquals(sql"abc".query[Int].unique.transact(xa(i)).attempt.unsafeRunSync().toOption, None)
    assertEquals(i.Connection.rollback, Some(()))
  }

  test("[Streaming] Connection.autoCommit should be set to false") {
    val i = new Interp
    val _ = sql"select 1".query[Int].stream.compile.toList.transact(xa(i)).unsafeRunSync()
    assertEquals(i.Connection.autoCommit, Some(false))
  }

  test("[Streaming] Connection.commit should be called on success") {
    val i = new Interp
    val _ = sql"select 1".query[Int].stream.compile.toList.transact(xa(i)).unsafeRunSync()
    assertEquals(i.Connection.commit, Some(()))
  }

  test("[Streaming] Connection.commit should NOT be called on failure") {
    val i = new Interp
    assertEquals(sql"abc".query[Int].stream.compile.toList.transact(xa(i)).attempt.unsafeRunSync().toOption, None)
    assertEquals(i.Connection.commit, None)
  }

  test("[Streaming] Connection.rollback should NOT be called on success") {
    val i = new Interp
    val _ = sql"select 1".query[Int].stream.compile.toList.transact(xa(i)).unsafeRunSync()
    assertEquals(i.Connection.rollback, None)
  }

  test("[Streaming] Connection.rollback should be called on failure") {
    val i = new Interp
    assertEquals(sql"abc".query[Int].stream.compile.toList.transact(xa(i)).attempt.unsafeRunSync().toOption, None)
    assertEquals(i.Connection.rollback, Some(()))
  }

  test("PreparedStatement.close should be called on success") {
    val i = new Interp
    val _ = sql"select 1".query[Int].unique.transact(xa(i)).unsafeRunSync()
    assertEquals(i.PreparedStatement.close, Some(()))
  }

  test("PreparedStatement.close should be called on failure") {
    val i = new Interp
    assertEquals(sql"select 'x'".query[Int].unique.transact(xa(i)).attempt.unsafeRunSync().toOption, None)
    assertEquals(i.PreparedStatement.close, Some(()))
  }

  test("[Streaming] PreparedStatement.close should be called on success") {
    val i = new Interp
    val _ = sql"select 1".query[Int].stream.compile.toList.transact(xa(i)).unsafeRunSync()
    assertEquals(i.PreparedStatement.close, Some(()))
  }

  test("[Streaming] PreparedStatement.close should be called on failure") {
    val i = new Interp
    assertEquals(
      sql"select 'x'".query[Int].stream.compile.toList.transact(xa(i)).attempt.unsafeRunSync().toOption,
      None)
    assertEquals(i.PreparedStatement.close, Some(()))
  }

  test("ResultSet.close should be called on success") {
    val i = new Interp
    val _ = sql"select 1".query[Int].unique.transact(xa(i)).unsafeRunSync()
    assertEquals(i.ResultSet.close, Some(()))
  }

  test("ResultSet.close should be called on failure") {
    val i = new Interp
    assertEquals(sql"select 'x'".query[Int].unique.transact(xa(i)).attempt.unsafeRunSync().toOption, None)
    assertEquals(i.ResultSet.close, Some(()))
  }

  test("[Streaming] ResultSet.close should be called on success") {
    val i = new Interp
    val _ = sql"select 1".query[Int].stream.compile.toList.transact(xa(i)).unsafeRunSync()
    assertEquals(i.ResultSet.close, Some(()))
  }

  test("[Streaming] ResultSet.close should be called on failure") {
    val i = new Interp
    assertEquals(
      sql"select 'x'".query[Int].stream.compile.toList.transact(xa(i)).attempt.unsafeRunSync().toOption,
      None)
    assertEquals(i.ResultSet.close, Some(()))
  }

}
