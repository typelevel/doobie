// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.issue

import doobie._, doobie.implicits._
import org.specs2.mutable.Specification
import scala.Predef._
import shapeless.{::, HNil}

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object `780` extends Specification {

  "Param" should {
    "exist when meta instances are in scope" in {
      class Foo[A: Meta, B: Meta] {
        def bar = Param[A :: B :: HNil]
      }

      true
    }
  }

}
