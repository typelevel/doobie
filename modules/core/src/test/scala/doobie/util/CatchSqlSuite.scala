// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.{ Effect, IO }
import cats.effect.syntax.effect._
import cats.syntax.applicativeError._
import doobie._, doobie.implicits._
import java.sql.SQLException

class CatchSqlSuiteIO extends CatchSqlSuite[IO] {
  implicit val E: Effect[IO] = IO.ioEffect
}

trait CatchSqlSuite[F[_]] extends munit.FunSuite {

  implicit def E: Effect[F]

  val SQLSTATE_FOO = SqlState("Foo")
  val SQLSTATE_BAR = SqlState("Bar")

  test("attemptSql should do nothing on success") {
    assertEquals(E.delay(3).attemptSql.toIO.unsafeRunSync(), Right(3))
  }

  test("attemptSql should catch SQLException") {
    val e = new SQLException
    assertEquals(E.raiseError(e).attemptSql.toIO.unsafeRunSync(), Left(e))
  }

  test("attemptSql should ignore non-SQLException") {
    val e = new IllegalArgumentException
    intercept[IllegalArgumentException] {
      E.raiseError(e).attemptSql.toIO.unsafeRunSync()
    }
  }

  test("attemptSqlState shuold do nothing on success") {
    assertEquals(E.delay(3).attemptSqlState.toIO.unsafeRunSync(), Right(3))
  }

  test("attemptSqlState shuold catch SQLException") {
    val e = new SQLException("", SQLSTATE_FOO.value)
    assertEquals(E.raiseError(e).attemptSqlState.toIO.unsafeRunSync(), Left(SQLSTATE_FOO))
  }

  test("attemptSqlState shuold ignore non-SQLException") {
    val e = new IllegalArgumentException
    intercept[IllegalArgumentException] {
      E.raiseError(e).attemptSqlState.toIO.unsafeRunSync()
    }
  }

  test("attemptSomeSqlState should do nothing on success") {
    assertEquals(E.delay(3).attemptSomeSqlState {
      case SQLSTATE_FOO => 42
      case SQLSTATE_BAR => 66
    }.toIO.unsafeRunSync(), Right(3))
  }

  test("attemptSomeSqlState should catch SQLException with matching state (1)") {
    val e = new SQLException("", SQLSTATE_FOO.value)
    assertEquals(E.raiseError(e).attemptSomeSqlState {
      case SQLSTATE_FOO => 42
      case SQLSTATE_BAR => 66
    }.toIO.unsafeRunSync(), Left(42))
  }

  test("attemptSomeSqlState should catch SQLException with matching state (2)") {
    val e = new SQLException("", SQLSTATE_BAR.value)
    assertEquals(E.raiseError(e).attemptSomeSqlState {
      case SQLSTATE_FOO => 42
      case SQLSTATE_BAR => 66
    }.toIO.unsafeRunSync(), Left(66))
  }

  test("attemptSomeSqlState should ignore SQLException with non-matching state") {
    val e = new SQLException("", SQLSTATE_BAR.value)
    intercept[SQLException] {
      E.raiseError(e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
      }.toIO.unsafeRunSync()
    }
  }

  test("attemptSomeSqlState should ignore non-SQLException") {
    val e = new IllegalArgumentException
    intercept[IllegalArgumentException] {
      E.raiseError(e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
      }.toIO.unsafeRunSync()
    }
  }

  lazy val rescue = E.delay(4)

  test("exceptSql should do nothing on success") {
    assertEquals(E.delay(3).exceptSql(_ => rescue).toIO.unsafeRunSync(), 3)
  }

  test("exceptSql should catch SQLException") {
    val e = new SQLException("", SQLSTATE_FOO.value)
    assertEquals(E.raiseError[Int](e).exceptSql(_ => rescue).toIO.unsafeRunSync(), 4)
  }

  test("exceptSql should ignore non-SQLException") {
    val e = new IllegalArgumentException
    intercept[IllegalArgumentException] {
      E.raiseError[Int](e).exceptSql(_ => rescue).toIO.unsafeRunSync()
    }
  }

  test("exceptSqlState should do nothing on success") {
    assertEquals(E.delay(3).exceptSqlState(_ => rescue).toIO.unsafeRunSync(), 3)
  }

  test("exceptSqlState should catch SQLException") {
    val e = new SQLException("", SQLSTATE_FOO.value)
    assertEquals(E.raiseError[Int](e).exceptSqlState(_ => rescue).toIO.unsafeRunSync(), 4)
  }

  test("exceptSqlState should ignore non-SQLException") {
    val e = new IllegalArgumentException
    intercept[IllegalArgumentException] {
      E.raiseError[Int](e).exceptSqlState(_ => rescue).toIO.unsafeRunSync()
    }
  }

  test("exceptSomeSqlState should do nothing on success") {
    assertEquals(E.delay(3).exceptSomeSqlState { case _ => rescue }.toIO.unsafeRunSync(), 3)
  }

  test("exceptSomeSqlState should catch SQLException with some state") {
    val e = new SQLException("", SQLSTATE_FOO.value)
    assertEquals(E.raiseError[Int](e).exceptSomeSqlState { case SQLSTATE_FOO => rescue }.toIO.unsafeRunSync(), 4)
  }

  test("exceptSomeSqlState should ignore SQLException with other state") {
    val e = new SQLException("", SQLSTATE_FOO.value)
    intercept[SQLException] {
      E.raiseError[Int](e).exceptSomeSqlState { case SQLSTATE_BAR => rescue }.toIO.unsafeRunSync()
    }
  }

  test("exceptSomeSqlState should ignore non-SQLException") {
    val e = new IllegalArgumentException
    intercept[IllegalArgumentException] {
      E.raiseError[Int](e).exceptSomeSqlState { case _ => rescue }.toIO.unsafeRunSync()
    }
  }

  test("onSqlException should do nothing on success") {
    var a = 1
    E.delay(3).onSqlException(E.delay(a += 1)).attempt.toIO.unsafeRunSync()
    assertEquals(a, 1)
  }

  test("onSqlException should perform its effect on SQLException") {
    var a = 1
    val e = new SQLException("", SQLSTATE_FOO.value)
    assertEquals(E.raiseError[Int](e).onSqlException(E.delay(a += 1)).attempt.toIO.unsafeRunSync(), Left(e))
    assertEquals(a, 2)
  }

  test("onSqlException should ignore its effect on non-SQLException") {
    var a = 1
    val e = new IllegalArgumentException
    assertEquals(E.raiseError[Int](e).onSqlException(E.delay(a += 1)).attempt.toIO.unsafeRunSync(), Left(e))
    assertEquals(a, 1)
  }

}

