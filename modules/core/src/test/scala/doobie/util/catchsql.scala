// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.IO
import doobie._, doobie.implicits._
import java.sql.SQLException
import org.specs2.mutable.Specification


class catchsqlspec extends Specification {

  import cats.effect.unsafe.implicits.global

  val SQLSTATE_FOO = SqlState("Foo")
  val SQLSTATE_BAR = SqlState("Bar")

  "attemptSql" should {

    "do nothing on success" in {
      IO.delay(3).attemptSql.unsafeRunSync() must_== Right(3)
    }

    "catch SQLException" in {
      val e = new SQLException
      IO.raiseError(e).attemptSql.unsafeRunSync() must_== Left(e)
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      IO.raiseError(e).attemptSql.unsafeRunSync() must throwA[IllegalArgumentException]
    }

  }


  "attemptSqlState" should {

    "do nothing on success" in {
      IO.delay(3).attemptSqlState.unsafeRunSync() must_== Right(3)
    }

    "catch SQLException" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      IO.raiseError(e).attemptSqlState.unsafeRunSync() must_== Left(SQLSTATE_FOO)
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      IO.raiseError(e).attemptSqlState.unsafeRunSync() must throwA[IllegalArgumentException]
    }

  }

  "attemptSomeSqlState" should {

    "do nothing on success" in {
      IO.delay(3).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
        case SQLSTATE_BAR        => 66
      }.unsafeRunSync() must_== Right(3)
    }

    "catch SQLException with matching state (1)" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      IO.raiseError(e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
        case SQLSTATE_BAR        => 66
      }.unsafeRunSync() must_== Left(42)
    }

    "catch SQLException with matching state (2)" in {
      val e = new SQLException("", SQLSTATE_BAR.value)
      IO.raiseError(e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
        case SQLSTATE_BAR        => 66
      }.unsafeRunSync() must_== Left(66)
    }

    "ignore SQLException with non-matching state" in {
      val e = new SQLException("", SQLSTATE_BAR.value)
      IO.raiseError(e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
      }.unsafeRunSync() must throwA[SQLException]
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      IO.raiseError(e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
      }.unsafeRunSync() must throwA[IllegalArgumentException]
    }

  }

  "exceptSql" should {

    val rescue = IO.delay(4)

    "do nothing on success" in {
      IO.delay(3).exceptSql(_ => rescue).unsafeRunSync() must_== 3
    }

    "catch SQLException" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      IO.raiseError[Int](e).exceptSql(_ => rescue).unsafeRunSync() must_== 4
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      IO.raiseError[Int](e).exceptSql(_ => rescue).unsafeRunSync() must throwA[IllegalArgumentException]
    }

  }

  "exceptSqlState" should {

    val rescue = IO.delay(4)

    "do nothing on success" in {
      IO.delay(3).exceptSqlState(_ => rescue).unsafeRunSync() must_== 3
    }

    "catch SQLException" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      IO.raiseError[Int](e).exceptSqlState(_ => rescue).unsafeRunSync() must_== 4
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      IO.raiseError[Int](e).exceptSqlState(_ => rescue).unsafeRunSync() must throwA[IllegalArgumentException]
    }

  }

  "exceptSomeSqlState" should {

    val rescue = IO.delay(4)

    "do nothing on success" in {
      IO.delay(3).exceptSomeSqlState { case _ => rescue }.unsafeRunSync() must_== 3
    }

    "catch SQLException with some state" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      IO.raiseError[Int](e).exceptSomeSqlState { case SQLSTATE_FOO => rescue }.unsafeRunSync() must_== 4
    }

    "ignore SQLException with other state" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      IO.raiseError[Int](e).exceptSomeSqlState { case SQLSTATE_BAR => rescue }.unsafeRunSync() must throwA[SQLException]
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      IO.raiseError[Int](e).exceptSomeSqlState { case _ => rescue }.unsafeRunSync() must throwA[IllegalArgumentException]
    }

  }

  "onSqlException" should {

    "do nothing on success" in {
      var a = 1
      IO.delay(3).onSqlException(IO.delay(a += 1)).attempt.unsafeRunSync()
      a must_== 1
    }

    "perform its effect on SQLException" in {
      var a = 1
      val e = new SQLException("", SQLSTATE_FOO.value)
      IO.raiseError[Int](e).onSqlException(IO.delay(a += 1)).attempt.unsafeRunSync() must_== Left(e)
      a must_== 2
    }

    "ignore its effect on non-SQLException" in {
      var a = 1
      val e = new IllegalArgumentException
      IO.raiseError[Int](e).onSqlException(IO.delay(a += 1)).attempt.unsafeRunSync() must_== Left(e)
      a must_== 1
    }

  }

}

