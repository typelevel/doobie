// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie
package util

import shapeless._, shapeless.test._
import org.specs2.mutable.Specification


object paramspec extends Specification {

  final case class Z(i: Int, s: String)
  object S

  "Param" should {

    "exist for HNil" in {
      Param[HNil]
      true
    }

    "exist for A :: HNil given Put[A]" in {
      def foo[A: Put] = Param[A :: HNil]
      true
    }

    "exist for Option[A] :: HNil given Put[A]" in {
      def foo[A: Put] = Param[Option[A] :: HNil]
      true
    }

    "exist for any HList with Put for head" in {
      def foo[A: Put, B <: HList : Param] = Param[A :: B]
      true
    }

    "exist for any HList with Option of Put for head" in {
      def foo[A: Put, B <: HList:  Param] = Param[Option[A] :: B]
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
