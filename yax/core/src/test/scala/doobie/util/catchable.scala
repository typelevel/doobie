package doobie.util

#+scalaz
import scalaz.{ -\/, \/- }
#-scalaz
#+cats
import scala.util.{ Left => -\/, Right => \/- }
#-cats

import doobie.util.iolite.IOLite
import doobie.syntax.catchable._

import org.specs2.mutable.Specification

object catchablespec extends Specification with ToDoobieCatchableOps {

  "attemptSome" should {

    "do nothing on success" in {
      IOLite.primitive(3).attemptSome {
        case _ => 4
      }.unsafePerformIO must_== \/-(3)
    }

    "catch matching throwables" in {
      IOLite.primitive(throw new IllegalArgumentException).attemptSome {
        case ie: IllegalArgumentException => 42
      }.unsafePerformIO must_== -\/(42)
    }

    "ignore non-matching throwables" in {      
      IOLite.primitive(throw new IllegalArgumentException).attemptSome {
        case ise: IllegalStateException => 42
      }.unsafePerformIO must throwA[IllegalArgumentException]
    }

  }

  "except" should {

    "do nothing on success" in {
      IOLite.primitive(3).except(t => IOLite.primitive(4)).unsafePerformIO must_== 3
    }

    "catch all exceptions" in {
      IOLite.primitive[Int](Predef.???).except(t => IOLite.primitive(4)).unsafePerformIO must_== 4
    }

  }

  "exceptSome" should {

    "do nothing on success" in {
      IOLite.primitive(3).exceptSome {
        case _ => IOLite.primitive(4)
      }.unsafePerformIO must_== 3
    }

    "catch matching throwables" in {
      IOLite.primitive[Int](throw new IllegalArgumentException).exceptSome {
        case ie: IllegalArgumentException => IOLite.primitive(42)
      }.unsafePerformIO must_== 42
    }

    "ignore non-matching throwables" in {      
      IOLite.primitive[Int](throw new IllegalArgumentException).exceptSome {
        case ise: IllegalStateException => IOLite.primitive(42)
      }.unsafePerformIO must throwA[IllegalArgumentException]
    }

  }

  "onException" should {

    "do nothing on success" in {
      var a = 1
      IOLite.primitive(42).onException(IOLite.primitive(a += 1)).unsafePerformIO
      a must_== 1
    }

    "perform its effect on exception" in {
      var a = 1
      try {
        IOLite.primitive[Int](Predef.???).onException(IOLite.primitive(a += 1)).unsafePerformIO
        false
      } catch {
        case _: Throwable => a == 2
      }      
    }

  }

  "ensuring" should {

    "perform its effect on success" in {
      var a = 1
      IOLite.primitive(42).ensuring(IOLite.primitive(a += 1)).unsafePerformIO
      a must_== 2
    }

    "perform its effect on exception" in {
      var a = 1
      try {
        IOLite.primitive[Int](Predef.???).ensuring(IOLite.primitive(a += 1)).unsafePerformIO
        false
      } catch {
        case _: Throwable => a == 2
      }      
    }

  }


}




