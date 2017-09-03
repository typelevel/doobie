package doobie.util

import cats.effect.IO
import doobie._, doobie.implicits._
import java.sql.SQLException
import org.specs2.mutable.Specification

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Var"))
object catchsqlpec extends Specification {

  val SQLSTATE_FOO = SqlState("Foo")
  val SQLSTATE_BAR = SqlState("Bar")

  "attemptSql" should {

    "do nothing on success" in {
      IO(3).attemptSql.unsafeRunSync must_== Right(3)
    }

    "catch SQLException" in {
      val e = new SQLException
      IO(throw e).attemptSql.unsafeRunSync must_== Left(e)
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      IO(throw e).attemptSql.unsafeRunSync must throwA[IllegalArgumentException]
    }

  }


  "attemptSqlState" should {

    "do nothing on success" in {
      IO(3).attemptSqlState.unsafeRunSync must_== Right(3)
    }

    "catch SQLException" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      IO(throw e).attemptSqlState.unsafeRunSync must_== Left(SQLSTATE_FOO)
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      IO(throw e).attemptSqlState.unsafeRunSync must throwA[IllegalArgumentException]
    }

  }

  "attemptSomeSqlState" should {

    "do nothing on success" in {
      IO(3).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
        case SQLSTATE_BAR        => 66
      }.unsafeRunSync must_== Right(3)
    }

    "catch SQLException with matching state (1)" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      IO(throw e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
        case SQLSTATE_BAR        => 66
      }.unsafeRunSync must_== Left(42)
    }

    "catch SQLException with matching state (2)" in {
      val e = new SQLException("", SQLSTATE_BAR.value)
      IO(throw e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
        case SQLSTATE_BAR        => 66
      }.unsafeRunSync must_== Left(66)
    }

    "ignore SQLException with non-matching state" in {
      val e = new SQLException("", SQLSTATE_BAR.value)
      IO(throw e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
      }.unsafeRunSync must throwA[SQLException]
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      IO(throw e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
      }.unsafeRunSync must throwA[IllegalArgumentException]
    }

  }

  "exceptSql" should {

    val rescue = IO(4)

    "do nothing on success" in {
      IO(3).exceptSql(e => rescue).unsafeRunSync must_== 3
    }

    "catch SQLException" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      IO[Int](throw e).exceptSql(e => rescue).unsafeRunSync must_== 4
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      IO[Int](throw e).exceptSql(e => rescue).unsafeRunSync must throwA[IllegalArgumentException]
    }

  }

  "exceptSqlState" should {

    val rescue = IO(4)

    "do nothing on success" in {
      IO(3).exceptSqlState(e => rescue).unsafeRunSync must_== 3
    }

    "catch SQLException" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      IO[Int](throw e).exceptSqlState(e => rescue).unsafeRunSync must_== 4
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      IO[Int](throw e).exceptSqlState(e => rescue).unsafeRunSync must throwA[IllegalArgumentException]
    }

  }

  "exceptSomeSqlState" should {

    val rescue = IO(4)

    "do nothing on success" in {
      IO(3).exceptSomeSqlState { case _ => rescue }.unsafeRunSync must_== 3
    }

    "catch SQLException with some state" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      IO[Int](throw e).exceptSomeSqlState { case SQLSTATE_FOO => rescue }.unsafeRunSync must_== 4
    }

    "ignore SQLException with other state" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      IO[Int](throw e).exceptSomeSqlState { case SQLSTATE_BAR => rescue }.unsafeRunSync must throwA[SQLException]
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      IO[Int](throw e).exceptSomeSqlState { case _ => rescue }.unsafeRunSync must throwA[IllegalArgumentException]
    }

  }

  "onSqlException" should {

    "do nothing on success" in {
      var a = 1
      IO(3).onSqlException(IO(a += 1)).attempt.unsafeRunSync
      a must_== 1
    }

    "perform its effect on SQLException" in {
      var a = 1
      val e = new SQLException("", SQLSTATE_FOO.value)
      IO[Int](throw e).onSqlException(IO(a += 1)).attempt.unsafeRunSync must_== Left(e)
      a must_== 2
    }

    "ignore its effect on non-SQLException" in {
      var a = 1
      val e = new IllegalArgumentException
      IO[Int](throw e).onSqlException(IO(a += 1)).attempt.unsafeRunSync must_== Left(e)
      a must_== 1
    }

  }

}
