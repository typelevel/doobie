// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import org.specs2.mutable.Specification


class metaspec extends Specification {

  "Meta" should {

    "exist for primitive types" in {
      Meta[Int]
      Meta[String]
      true
    }

    "imply Get" in {
      def foo[A: Meta] = Get[A]
      true
    }

    "imply Put" in {
      def foo[A: Meta] = Put[A]
      true
    }

  }

}