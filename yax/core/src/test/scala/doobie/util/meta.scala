package doobie.util

import shapeless._, shapeless.test._
import scala.Predef.classOf
import doobie.imports._
import doobie.enum.jdbctype._
import org.specs2.mutable.Specification

object metaspec extends Specification {
  case class X(x: Int)
  case class Y(x: String) extends AnyVal
  case class P(x: Int) extends AnyVal
  case class Q(x: String)

  case class Z(i: Int, s: String)
  object S

  case class Reg1(x: Int)
  case class Reg2(x: Int)

  "Meta" should {

    "exist for primitive types" in {
      Meta[Int]
      Meta[String]

      true
    }

    "be derived for unary products" in {
      Meta[X]
      Meta[Y]
      Meta[P]
      Meta[Q]

      true
    }

    "not be derived for non-unary products" in {
      illTyped("Meta[Z]")
      illTyped("Meta[(Int, Int)]")
      illTyped("Meta[S.type]")

      true
    }

    "register new instances" in {
      Meta[Reg1]
      Meta.readersOf(Integer, "").filter(_.scalaType == "doobie.util.metaspec.Reg1").size must_== 1
    }

    "not register multiple equivalent instances" in {
      Meta[Reg2]
      Meta[Reg2]
      Meta[Reg2]
      Meta.readersOf(Integer, "").filter(_.scalaType == "doobie.util.metaspec.Reg2").size must_== 1
    }

  }

}
