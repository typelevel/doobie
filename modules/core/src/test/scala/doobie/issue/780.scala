// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.issue

import doobie._
import org.specs2.mutable.Specification
import shapeless.{::, HNil}


class `780` extends Specification {

  "deriving instances" should {
    "work correctly for Write from class scope" in {
      class Foo[A: Write, B: Write] {
        Write[A :: B :: HNil]
      }
      true
    }
  }

}
