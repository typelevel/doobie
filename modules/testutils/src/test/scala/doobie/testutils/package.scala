// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie

// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

import munit.Location
import munit.Assertions.assert

import scala.annotation.nowarn

package object testutils {
  implicit class VoidExtensions(val a: Any) extends AnyVal {
    @nowarn
    def void: Unit = {
      val _ = a
    }
  }

  def assertContains(obtained: String, expected: String)(implicit loc: Location): Unit = {
    assert(obtained.contains(expected), s"$obtained did not contain $expected")
  }
}
