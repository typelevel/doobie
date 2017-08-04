package doobie.util

import scala.util.{ Left => -\/, Right => \/- }
import doobie.util.iolite.IOLite
import doobie.enum.sqlstate.SqlState
import doobie.syntax.catchsql._

import org.specs2.mutable.Specification
import java.sql.SQLException

object catchsqlpec extends Specification with ToDoobieCatchSqlOps {

  val SQLSTATE_FOO = SqlState("Foo")
  val SQLSTATE_BAR = SqlState("Bar")

  "attemptSql" should {

    "do nothing on success" in {
      IOLite.primitive(3).attemptSql.unsafePerformIO must_== \/-(3)
    }

    "catch SQLException" in {
      val e = new SQLException
      IOLite.primitive(throw e).attemptSql.unsafePerformIO must_== -\/(e)
    }

    "ignore non-SQLException" in {      
      val e = new IllegalArgumentException
      IOLite.primitive(throw e).attemptSql.unsafePerformIO must throwA[IllegalArgumentException]
    }

  }


  "attemptSqlState" should {

    "do nothing on success" in {
      IOLite.primitive(3).attemptSqlState.unsafePerformIO must_== \/-(3)
    }

    "catch SQLException" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      IOLite.primitive(throw e).attemptSqlState.unsafePerformIO must_== -\/(SQLSTATE_FOO)
    }

    "ignore non-SQLException" in {      
      val e = new IllegalArgumentException
      IOLite.primitive(throw e).attemptSqlState.unsafePerformIO must throwA[IllegalArgumentException]
    }

  }
  
  "attemptSomeSqlState" should {

    "do nothing on success" in {
      IOLite.primitive(3).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
        case SQLSTATE_BAR        => 66
      }.unsafePerformIO must_== \/-(3)
    }

    "catch SQLException with matching state (1)" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      IOLite.primitive(throw e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
        case SQLSTATE_BAR        => 66
      }.unsafePerformIO must_== -\/(42)
    }

    "catch SQLException with matching state (2)" in {
      val e = new SQLException("", SQLSTATE_BAR.value)
      IOLite.primitive(throw e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
        case SQLSTATE_BAR        => 66
      }.unsafePerformIO must_== -\/(66)
    }

    "ignore SQLException with non-matching state" in {
      val e = new SQLException("", SQLSTATE_BAR.value)
      IOLite.primitive(throw e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
      }.unsafePerformIO must throwA[SQLException]
    }

    "ignore non-SQLException" in {
      val e = new IllegalArgumentException
      IOLite.primitive(throw e).attemptSomeSqlState {
        case SQLSTATE_FOO => 42
      }.unsafePerformIO must throwA[IllegalArgumentException]
    }

  }

  "exceptSql" should {

    val rescue = IOLite.primitive(4)

    "do nothing on success" in {
      IOLite.primitive(3).exceptSql(e => rescue).unsafePerformIO must_== 3
    }

    "catch SQLException" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      IOLite.primitive[Int](throw e).exceptSql(e => rescue).unsafePerformIO must_== 4
    }

    "ignore non-SQLException" in {      
      val e = new IllegalArgumentException
      IOLite.primitive[Int](throw e).exceptSql(e => rescue).unsafePerformIO must throwA[IllegalArgumentException]
    }

  }

  "exceptSqlState" should {

    val rescue = IOLite.primitive(4)

    "do nothing on success" in {
      IOLite.primitive(3).exceptSqlState(e => rescue).unsafePerformIO must_== 3
    }

    "catch SQLException" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      IOLite.primitive[Int](throw e).exceptSqlState(e => rescue).unsafePerformIO must_== 4
    }

    "ignore non-SQLException" in {      
      val e = new IllegalArgumentException
      IOLite.primitive[Int](throw e).exceptSqlState(e => rescue).unsafePerformIO must throwA[IllegalArgumentException]
    }

  }

  "exceptSomeSqlState" should {

    val rescue = IOLite.primitive(4)

    "do nothing on success" in {
      IOLite.primitive(3).exceptSomeSqlState { case _ => rescue }.unsafePerformIO must_== 3
    }

    "catch SQLException with some state" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      IOLite.primitive[Int](throw e).exceptSomeSqlState { case SQLSTATE_FOO => rescue }.unsafePerformIO must_== 4
    }

    "ignore SQLException with other state" in {
      val e = new SQLException("", SQLSTATE_FOO.value)
      IOLite.primitive[Int](throw e).exceptSomeSqlState { case SQLSTATE_BAR => rescue }.unsafePerformIO must throwA[SQLException]
    }

    "ignore non-SQLException" in {      
      val e = new IllegalArgumentException
      IOLite.primitive[Int](throw e).exceptSomeSqlState { case _ => rescue }.unsafePerformIO must throwA[IllegalArgumentException]
    }

  }

  "onSqlException" should {

    "do nothing on success" in {
      var a = 1
      IOLite.primitive(3).onSqlException(IOLite.primitive(a += 1)).attempt.unsafePerformIO
      a must_== 1
    }

    "perform its effect on SQLException" in {
      var a = 1
      val e = new SQLException("", SQLSTATE_FOO.value)
      IOLite.primitive[Int](throw e).onSqlException(IOLite.primitive(a += 1)).attempt.unsafePerformIO must_== -\/(e)
      a must_== 2
    }

    "ignore its effect on non-SQLException" in {      
      var a = 1
      val e = new IllegalArgumentException
      IOLite.primitive[Int](throw e).onSqlException(IOLite.primitive(a += 1)).attempt.unsafePerformIO must_== -\/(e)
      a must_== 1
    }

  }

}




