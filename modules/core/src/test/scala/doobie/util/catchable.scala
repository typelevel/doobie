package doobie.util

import scalaz._
import scalaz.concurrent._
import Scalaz._
import doobie.imports._
import org.specs2.mutable.Specification

object catchablespec extends Specification {

  "attemptSome" should {

    "do nothing on success" in {
      Task.delay(3).attemptSome {
        case _ => 4
      }.unsafePerformSync must_== \/-(3)
    }

    "catch matching throwables" in {
      Task.delay(throw new IllegalArgumentException).attemptSome {
        case ie: IllegalArgumentException => 42
      }.unsafePerformSync must_== -\/(42)
    }

    "ignore non-matching throwables" in {      
      Task.delay(throw new IllegalArgumentException).attemptSome {
        case ise: IllegalStateException => 42
      }.unsafePerformSync must throwA[IllegalArgumentException]
    }

  }

  "except" should {

    "do nothing on success" in {
      Task.delay(3).except(t => Task.delay(4)).unsafePerformSync must_== 3
    }

    "catch all exceptions" in {
      Task.delay[Int](Predef.???).except(t => Task.delay(4)).unsafePerformSync must_== 4
    }

  }

  "exceptSome" should {

    "do nothing on success" in {
      Task.delay(3).exceptSome {
        case _ => Task.delay(4)
      }.unsafePerformSync must_== 3
    }

    "catch matching throwables" in {
      Task.delay[Int](throw new IllegalArgumentException).exceptSome {
        case ie: IllegalArgumentException => Task.delay(42)
      }.unsafePerformSync must_== 42
    }

    "ignore non-matching throwables" in {      
      Task.delay[Int](throw new IllegalArgumentException).exceptSome {
        case ise: IllegalStateException => Task.delay(42)
      }.unsafePerformSync must throwA[IllegalArgumentException]
    }

  }

  "onException" should {

    "do nothing on success" in {
      var a = 1
      Task.delay(42).onException(Task.delay(a += 1)).unsafePerformSync
      a must_== 1
    }

    "perform its effect on exception" in {
      var a = 1
      try {
        Task.delay[Int](Predef.???).onException(Task.delay(a += 1)).unsafePerformSync
        false
      } catch {
        case _: Throwable => a == 2
      }      
    }

  }

  "ensuring" should {

    "perform its effect on success" in {
      var a = 1
      Task.delay(42).ensuring(Task.delay(a += 1)).unsafePerformSync
      a must_== 2
    }

    "perform its effect on exception" in {
      var a = 1
      try {
        Task.delay[Int](Predef.???).ensuring(Task.delay(a += 1)).unsafePerformSync
        false
      } catch {
        case _: Throwable => a == 2
      }      
    }

  }


}




