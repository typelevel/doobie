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
import munit.CatsEffectSuite

import java.sql.{Connection, PreparedStatement, ResultSet}
import java.util

class StrategySuite extends CatsEffectSuite {

  val baseXa: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None
  )

  // an instrumented interpreter
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
    IO.pure(new Interp).flatTap(i => sql"select 1".query[Int].unique.transact(xa(i))).map(_.Connection.autoCommit)
      .assertEquals(Some(false))
  }

  test("Connection.commit should be called on success") {
    IO.pure(new Interp).flatTap(i => sql"select 1".query[Int].unique.transact(xa(i))).map(_.Connection.commit)
      .assertEquals(Some(()))
  }

  test("Connection.commit should NOT be called on failure") {
    IO.pure(new Interp).flatTap(i =>
      sql"abc".query[Int].unique.transact(xa(i)).attempt.map(_.isLeft).assert).map(
      _.Connection.commit)
      .assertEquals(None)
  }

  test("Connection.rollback should NOT be called on success") {
    IO.pure(new Interp).flatTap(i => sql"select 1".query[Int].unique.transact(xa(i))).map(_.Connection.rollback)
      .assertEquals(None)
  }

  test("Connection.rollback should be called on failure") {
    IO.pure(new Interp).flatTap(i =>
      sql"abc".query[Int].unique.transact(xa(i)).attempt.map(_.isLeft).assert).map(
      _.Connection.rollback)
      .assertEquals(Some(()))
  }

  test("[Streaming] Connection.autoCommit should be set to false") {
    IO.pure(new Interp).flatTap(i => sql"select 1".query[Int].stream.compile.toList.transact(xa(i))).map(
      _.Connection.autoCommit).assertEquals(Some(false))
  }

  test("[Streaming] Connection.commit should be called on success") {
    IO.pure(new Interp).flatTap(i => sql"select 1".query[Int].stream.compile.toList.transact(xa(i))).map(
      _.Connection.commit).assertEquals(Some(()))
  }

  test("[Streaming] Connection.commit should NOT be called on failure") {
    IO.pure(new Interp).flatTap(i =>
      sql"abc".query[Int].stream.compile.toList.transact(xa(i)).attempt.map(_.isLeft).assert).map(
      _.Connection.commit)
      .assertEquals(None)
  }

  test("[Streaming] Connection.rollback should NOT be called on success") {
    IO.pure(new Interp).flatTap(i => sql"select 1".query[Int].stream.compile.toList.transact(xa(i))).map(
      _.Connection.rollback).assertEquals(None)
  }

  test("[Streaming] Connection.rollback should be called on failure") {
    IO.pure(new Interp).flatTap(i =>
      sql"abc".query[Int].stream.compile.toList.transact(xa(i)).attempt.map(_.isLeft).assert).map(
      _.Connection.rollback)
      .assertEquals(Some(()))
  }

  test("PreparedStatement.close should be called on success") {
    IO.pure(new Interp).flatTap(i => sql"select 1".query[Int].unique.transact(xa(i))).map(
      _.PreparedStatement.close).assertEquals(Some(()))
  }

  test("PreparedStatement.close should be called on failure") {
    IO.pure(new Interp).flatTap(i =>
      sql"select 'x'".query[Int].unique.transact(xa(i)).attempt.map(_.isLeft).assert).map(
      _.PreparedStatement.close)
      .assertEquals(Some(()))
  }

  test("[Streaming] PreparedStatement.close should be called on success") {
    IO.pure(new Interp).flatTap(i => sql"select 1".query[Int].stream.compile.toList.transact(xa(i))).map(
      _.PreparedStatement.close).assertEquals(Some(()))
  }

  test("[Streaming] PreparedStatement.close should be called on failure") {
    IO.pure(new Interp).flatTap(i =>
      sql"select 'x'".query[Int].stream.compile.toList.transact(xa(i)).attempt.map(_.isLeft).assert).map(
      _.PreparedStatement.close)
      .assertEquals(Some(()))
  }

  test("ResultSet.close should be called on success") {
    IO.pure(new Interp).flatTap(i => sql"select 1".query[Int].unique.transact(xa(i))).map(
      _.ResultSet.close).assertEquals(Some(()))
  }

  test("ResultSet.close should be called on failure") {
    IO.pure(new Interp).flatTap(i =>
      sql"select 'x'".query[Int].unique.transact(xa(i)).attempt.map(_.isLeft).assert).map(
      _.ResultSet.close)
      .assertEquals(Some(()))
  }

  test("[Streaming] ResultSet.close should be called on success") {
    IO.pure(new Interp).flatTap(i => sql"select 1".query[Int].stream.compile.toList.transact(xa(i))).map(
      _.ResultSet.close).assertEquals(Some(()))
  }

  test("[Streaming] ResultSet.close should be called on failure") {
    IO.pure(new Interp).flatTap(i =>
      sql"select 'x'".query[Int].stream.compile.toList.transact(xa(i)).attempt.map(_.isLeft).assert).map(
      _.ResultSet.close)
      .assertEquals(Some(()))
  }

}
