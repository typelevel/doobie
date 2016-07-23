#+scalaz
package doobie.util

import scalaz._, Scalaz._
import doobie.util.name._
import org.specs2.mutable.Specification

object namespec extends Specification {

   "Catchable[Name]" should {

    import effect.IO

    type F[A] = Name[A]
    val C = Catchable[F]
    val err = new Error("oh noes")
    val bad = C.fail[Int](err)

    "throw exceptions captured via fail()" in {
      try {
        bad.value
        sys.error("should have thrown")
      } catch {
        case t: Throwable => t must_== err
      }
    }

    "catch exceptions captured via fail()" in {
      C.attempt(bad).value must_== -\/(err)
    }

    "catch ambient exceptions (1/3)" in {
      C.attempt(Name[Int](throw err)).value must_== -\/(err)
    }

    "catch ambient exceptions (2/3)" in {
      C.attempt(Name(1).map(n => throw err)).value must_== -\/(err)
    }

    "catch ambient exceptions (3/3)" in {
      C.attempt(Name(1).flatMap(n => throw err)).value must_== -\/(err)
    }

    "properly handle success" in {
      C.attempt(Name(3)).value must_== \/-(3)
    }

  }


}
#-scalaz
