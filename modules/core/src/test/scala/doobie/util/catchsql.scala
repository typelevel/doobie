// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.{ Effect, IO }
import cats.effect.syntax.effect._
import cats.syntax.applicativeError._
import doobie._, doobie.implicits._
import java.sql.SQLException
import org.specs2.mutable.Specification


trait catchsqlspec[F[_]] extends Specification {

  implicit def E: Effect[F]

  val SQLSTATE_FOO = SqlState("Foo")
  val SQLSTATE_BAR = SqlState("Bar")

  "attemptSql" should {

    "do nothing on success" in {
      E.delay(3).attemptSql.toIO.unsafeRunSync must_== Right(3)
    }

    "catch SQLException" in {
      val e = new SQLException
      E.raiseError(e).attemptSql.toIO.unsafeRunSync must_== Left(e)
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      E.raiseError(e).attemptSql.toIO.unsafeRunSync must throwA[IllegalArgumentException]
    }

  }


  "attemptSqlState" should {

    "do nothing on success" in {
      E.delay(3).attemptSqlState.toIO.unsafeRunSync must_== Right(3)
    }

    "catch SQLException" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      E.raiseError(e).attemptSqlState.toIO.unsafeRunSync must_== Left(SQLSTATE_FOO)
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      E.raiseError(e).attemptSqlState.toIO.unsafeRunSync must throwA[IllegalArgumentException]
    }

  }

  "attemptSomeSqlState" should {

    "do nothing on success" in {
      E.delay(3).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
        case SQLSTATE_BAR        => 66
      }.toIO.unsafeRunSync must_== Right(3)
    }

    "catch SQLException with matching state (1)" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      E.raiseError(e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
        case SQLSTATE_BAR        => 66
      }.toIO.unsafeRunSync must_== Left(42)
    }

    "catch SQLException with matching state (2)" in {
      val e = new SQLException("", SQLSTATE_BAR.value)
      E.raiseError(e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
        case SQLSTATE_BAR        => 66
      }.toIO.unsafeRunSync must_== Left(66)
    }

    "ignore SQLException with non-matching state" in {
      val e = new SQLException("", SQLSTATE_BAR.value)
      E.raiseError(e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
      }.toIO.unsafeRunSync must throwA[SQLException]
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      E.raiseError(e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
      }.toIO.unsafeRunSync must throwA[IllegalArgumentException]
    }

  }

  "exceptSql" should {

    val rescue = E.delay(4)

    "do nothing on success" in {
      E.delay(3).exceptSql(_ => rescue).toIO.unsafeRunSync must_== 3
    }

    "catch SQLException" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      E.raiseError[Int](e).exceptSql(_ => rescue).toIO.unsafeRunSync must_== 4
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      E.raiseError[Int](e).exceptSql(_ => rescue).toIO.unsafeRunSync must throwA[IllegalArgumentException]
    }

  }

  "exceptSqlState" should {

    val rescue = E.delay(4)

    "do nothing on success" in {
      E.delay(3).exceptSqlState(_ => rescue).toIO.unsafeRunSync must_== 3
    }

    "catch SQLException" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      E.raiseError[Int](e).exceptSqlState(_ => rescue).toIO.unsafeRunSync must_== 4
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      E.raiseError[Int](e).exceptSqlState(_ => rescue).toIO.unsafeRunSync must throwA[IllegalArgumentException]
    }

  }

  "exceptSomeSqlState" should {

    val rescue = E.delay(4)

    "do nothing on success" in {
      E.delay(3).exceptSomeSqlState { case _ => rescue }.toIO.unsafeRunSync must_== 3
    }

    "catch SQLException with some state" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      E.raiseError[Int](e).exceptSomeSqlState { case SQLSTATE_FOO => rescue }.toIO.unsafeRunSync must_== 4
    }

    "ignore SQLException with other state" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      E.raiseError[Int](e).exceptSomeSqlState { case SQLSTATE_BAR => rescue }.toIO.unsafeRunSync must throwA[SQLException]
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      E.raiseError[Int](e).exceptSomeSqlState { case _ => rescue }.toIO.unsafeRunSync must throwA[IllegalArgumentException]
    }

  }

  "onSqlException" should {

    "do nothing on success" in {
      var a = 1
      E.delay(3).onSqlException(E.delay(a += 1)).attempt.toIO.unsafeRunSync
      a must_== 1
    }

    "perform its effect on SQLException" in {
      var a = 1
      val e = new SQLException("", SQLSTATE_FOO.value)
      E.raiseError[Int](e).onSqlException(E.delay(a += 1)).attempt.toIO.unsafeRunSync must_== Left(e)
      a must_== 2
    }

    "ignore its effect on non-SQLException" in {
      var a = 1
      val e = new IllegalArgumentException
      E.raiseError[Int](e).onSqlException(E.delay(a += 1)).attempt.toIO.unsafeRunSync must_== Left(e)
      a must_== 1
    }

  }

}

object catchsqlspecIO extends catchsqlspec[IO] {
  implicit val E: Effect[IO] = IO.ioEffect
}
