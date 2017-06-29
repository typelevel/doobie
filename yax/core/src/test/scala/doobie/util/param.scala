package doobie.util

import shapeless._, shapeless.test._
import doobie.imports._
import org.specs2.mutable.Specification

object paramspec extends Specification {

  case class Z(i: Int, s: String)
  object S

  "Param" should {

    "exist for HNil" in {
      Param[HNil]
      true
    }

    "exist for any Meta" in {
      def foo[A: Meta] = Param[A]
      true
    }

    "exist for any Option of Meta" in {
      def foo[A: Meta] = Param[Option[A]]
      true
    }


    "exist for any HList with Meta for head" in {
      def foo[A: Meta, B <: HList : Param] = Param[A :: B]
      true
    }

    "exist for any HList with Option of Meta for head" in {
      def foo[A: Meta, B <: HList:  Param] = Param[Option[A] :: B]
      true
    }

    "not exist for non-unary products" in {
      illTyped("Param[Z]")
      illTyped("Param[(Int, Int)]")
      illTyped("Param[S.type]")
      true
    }

  }

}
