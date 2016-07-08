package doobie.util

import scalaz._
import scalaz.concurrent._
import Scalaz._
import doobie.imports._
import org.specs2.mutable.Specification
import java.sql.SQLException

object catchsqlpec extends Specification {

  val SQLSTATE_FOO = SqlState("Foo")
  val SQLSTATE_BAR = SqlState("Bar")

  "attemptSql" should {

    "do nothing on success" in {
      Task.delay(3).attemptSql.unsafePerformSync must_== \/-(3)
    }

    "catch SQLException" in {
      val e = new SQLException
      Task.delay(throw e).attemptSql.unsafePerformSync must_== -\/(e)
    }

    "ignore non-SQLException" in {      
      val e = new IllegalArgumentException
      Task.delay(throw e).attemptSql.unsafePerformSync must throwA[IllegalArgumentException]
    }

  }


  "attemptSqlState" should {

    "do nothing on success" in {
      Task.delay(3).attemptSqlState.unsafePerformSync must_== \/-(3)
    }

    "catch SQLException" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      Task.delay(throw e).attemptSqlState.unsafePerformSync must_== -\/(SQLSTATE_FOO)
    }

    "ignore non-SQLException" in {      
      val e = new IllegalArgumentException
      Task.delay(throw e).attemptSqlState.unsafePerformSync must throwA[IllegalArgumentException]
    }

  }
  
  "attemptSomeSqlState" should {

    "do nothing on success" in {
      Task.delay(3).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
        case SQLSTATE_BAR        => 66
      }.unsafePerformSync must_== \/-(3)
    }

    "catch SQLException with matching state (1)" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      Task.delay(throw e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
        case SQLSTATE_BAR        => 66
      }.unsafePerformSync must_== -\/(42)
    }

    "catch SQLException with matching state (2)" in {
      val e = new SQLException("", SQLSTATE_BAR.value)
      Task.delay(throw e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
        case SQLSTATE_BAR        => 66
      }.unsafePerformSync must_== -\/(66)
    }

    "ignore SQLException with non-matching state" in {
      val e = new SQLException("", SQLSTATE_BAR.value)
      Task.delay(throw e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
      }.unsafePerformSync must throwA[SQLException]
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      Task.delay(throw e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
      }.unsafePerformSync must throwA[IllegalArgumentException]
    }

  }

  "exceptSql" should {

    val rescue = Task.delay(4)

    "do nothing on success" in {
      Task.delay(3).exceptSql(e => rescue).unsafePerformSync must_== 3
    }

    "catch SQLException" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      Task.delay[Int](throw e).exceptSql(e => rescue).unsafePerformSync must_== 4
    }

    "ignore non-SQLException" in {      
      val e = new IllegalArgumentException
      Task.delay[Int](throw e).exceptSql(e => rescue).unsafePerformSync must throwA[IllegalArgumentException]
    }

  }

  "exceptSqlState" should {

    val rescue = Task.delay(4)

    "do nothing on success" in {
      Task.delay(3).exceptSqlState(e => rescue).unsafePerformSync must_== 3
    }

    "catch SQLException" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      Task.delay[Int](throw e).exceptSqlState(e => rescue).unsafePerformSync must_== 4
    }

    "ignore non-SQLException" in {      
      val e = new IllegalArgumentException
      Task.delay[Int](throw e).exceptSqlState(e => rescue).unsafePerformSync must throwA[IllegalArgumentException]
    }

  }

  "exceptSomeSqlState" should {

    val rescue = Task.delay(4)

    "do nothing on success" in {
      Task.delay(3).exceptSomeSqlState { case _ => rescue }.unsafePerformSync must_== 3
    }

    "catch SQLException with some state" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      Task.delay[Int](throw e).exceptSomeSqlState { case SQLSTATE_FOO => rescue }.unsafePerformSync must_== 4
    }

    "ignore SQLException with other state" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      Task.delay[Int](throw e).exceptSomeSqlState { case SQLSTATE_BAR => rescue }.unsafePerformSync must throwA[SQLException]
    }

    "ignore non-SQLException" in {      
      val e = new IllegalArgumentException
      Task.delay[Int](throw e).exceptSomeSqlState { case _ => rescue }.unsafePerformSync must throwA[IllegalArgumentException]
    }

  }

  "onSqlException" should {

    "do nothing on success" in {
      var a = 1
      Task.delay(3).onSqlException(Task.delay(a += 1)).unsafePerformSyncAttempt
      a must_== 1
    }

    "perform its effect on SQLException" in {
      var a = 1
      val e = new SQLException("", SQLSTATE_FOO.value)
      Task.delay[Int](throw e).onSqlException(Task.delay(a += 1)).unsafePerformSyncAttempt must_== -\/(e)
      a must_== 2
    }

    "ignore its effect on non-SQLException" in {      
      var a = 1
      val e = new IllegalArgumentException
      Task.delay[Int](throw e).onSqlException(Task.delay(a += 1)).unsafePerformSyncAttempt must_== -\/(e)
      a must_== 1
    }

  }

}




