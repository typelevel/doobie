// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.{IO, Ref}
import doobie.*
import doobie.implicits.*
import munit.CatsEffectSuite

import java.sql.SQLException

class CatchSqlSuite extends CatsEffectSuite {

  val SQLSTATE_FOO = SqlState("Foo")
  val SQLSTATE_BAR = SqlState("Bar")

  test("attemptSql should do nothing on success") {
    IO.delay(3).attemptSql.assertEquals(Right(3))
  }

  test("attemptSql should catch SQLException") {
    val e = new SQLException
    IO.raiseError(e).attemptSql.assertEquals(Left(e))
  }

  test("attemptSql should ignore non-SQLException") {
    val e = new IllegalArgumentException
    IO.raiseError(e).attemptSql.intercept[IllegalArgumentException]
  }

  test("attemptSqlState shuold do nothing on success") {
    IO.delay(3).attemptSqlState.assertEquals(Right(3))
  }

  test("attemptSqlState shuold catch SQLException") {
    val e = new SQLException("", SQLSTATE_FOO.value)
    IO.raiseError(e).attemptSqlState.assertEquals(Left(SQLSTATE_FOO))
  }

  test("attemptSqlState shuold ignore non-SQLException") {
    val e = new IllegalArgumentException
    IO.raiseError(e).attemptSqlState.intercept[IllegalArgumentException]
  }

  test("attemptSomeSqlState should do nothing on success") {
    IO.delay(3).attemptSomeSqlState {
      case SQLSTATE_FOO => 42
      case SQLSTATE_BAR => 66
    }.assertEquals(Right(3))
  }

  test("attemptSomeSqlState should catch SQLException with matching state (1)") {
    val e = new SQLException("", SQLSTATE_FOO.value)
    IO.raiseError(e).attemptSomeSqlState {
      case SQLSTATE_FOO => 42
      case SQLSTATE_BAR => 66
    }.assertEquals(Left(42))
  }

  test("attemptSomeSqlState should catch SQLException with matching state (2)") {
    val e = new SQLException("", SQLSTATE_BAR.value)
    IO.raiseError(e).attemptSomeSqlState {
      case SQLSTATE_FOO => 42
      case SQLSTATE_BAR => 66
    }.assertEquals(Left(66))
  }

  test("attemptSomeSqlState should ignore SQLException with non-matching state") {
    val e = new SQLException("", SQLSTATE_BAR.value)
    IO.raiseError(e).attemptSomeSqlState {
      case SQLSTATE_FOO => 42
    }.intercept[SQLException]
  }

  test("attemptSomeSqlState should ignore non-SQLException") {
    val e = new IllegalArgumentException
    IO.raiseError(e).attemptSomeSqlState {
      case SQLSTATE_FOO => 42
    }.intercept[IllegalArgumentException]
  }

  lazy val rescue = IO.delay(4)

  test("exceptSql should do nothing on success") {
    IO.delay(3).exceptSql(_ => rescue).assertEquals(3)
  }

  test("exceptSql should catch SQLException") {
    val e = new SQLException("", SQLSTATE_FOO.value)
    IO.raiseError[Int](e).exceptSql(_ => rescue).assertEquals(4)
  }

  test("exceptSql should ignore non-SQLException") {
    val e = new IllegalArgumentException
    IO.raiseError[Int](e).exceptSql(_ => rescue).intercept[IllegalArgumentException]
  }

  test("exceptSqlState should do nothing on success") {
    IO.delay(3).exceptSqlState(_ => rescue).assertEquals(3)
  }

  test("exceptSqlState should catch SQLException") {
    val e = new SQLException("", SQLSTATE_FOO.value)
    IO.raiseError[Int](e).exceptSqlState(_ => rescue).assertEquals(4)
  }

  test("exceptSqlState should ignore non-SQLException") {
    val e = new IllegalArgumentException
    IO.raiseError[Int](e).exceptSqlState(_ => rescue).intercept[IllegalArgumentException]
  }

  test("exceptSomeSqlState should do nothing on success") {
    IO.delay(3).exceptSomeSqlState { case _ => rescue }.assertEquals(3)
  }

  test("exceptSomeSqlState should catch SQLException with some state") {
    val e = new SQLException("", SQLSTATE_FOO.value)
    IO.raiseError[Int](e).exceptSomeSqlState { case SQLSTATE_FOO => rescue }.assertEquals(4)
  }

  test("exceptSomeSqlState should ignore SQLException with other state") {
    val e = new SQLException("", SQLSTATE_FOO.value)
    IO.raiseError[Int](e).exceptSomeSqlState { case SQLSTATE_BAR => rescue }.intercept[SQLException]
  }

  test("exceptSomeSqlState should ignore non-SQLException") {
    val e = new IllegalArgumentException
    IO.raiseError[Int](e).exceptSomeSqlState { case _ => rescue }.intercept[IllegalArgumentException]
  }

  test("onSqlException should do nothing on success") {
    for {
      a <- Ref.of[IO, Int](1)
      _ <- IO.delay(3).onSqlException(IO.delay(a.set(2))).attempt
      b <- a.get
    } yield
    assertEquals(b, 1)
  }

  test("onSqlException should perform its effect on SQLException") {
    for {
      a <- Ref.of[IO, Int](1)
      e = new SQLException("foo")
      _ <- IO.raiseError[Int](e).onSqlException(a.set(2)).attempt.assertEquals(Left(e))
      b <- a.get
    } yield
      assertEquals(b, 2)
  }

  test("onSqlException should ignore its effect on non-SQLException") {
    for {
      a <- Ref.of[IO, Int](1)
      e = new RuntimeException("foo")
      _ <- IO.raiseError[Int](e).onSqlException(a.set(2)).attempt.assertEquals(Left(e))
      b <- a.get
    } yield {
      assertEquals(b, 1)
    }
  }

}
