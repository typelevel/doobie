package doobie.util

import shapeless._, shapeless.test._, shapeless.record._
import doobie.imports._
import org.specs2.mutable.Specification

object metaspec extends Specification {
  case class X(i: Int)
  case class Y(s: String) extends AnyVal

  case class Z(i: Int, s: String)
  object S

  "Meta" should {

    "exist for primitive types" in {
      Meta[Int]
      Meta[String]

      true
    }

    "be derived for unary products" in {
      Meta[X]
      Meta[Y]

      true
    }

    "not be derived for non-unary products" in {
      illTyped("Meta[Z]")
      illTyped("Meta[(Int, Int)]")
      illTyped("Meta[S.type]")

      true
    }

  }

}
