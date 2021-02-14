// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import doobie.Get

trait GetSuitePlatform { self: GetSuite =>
  case class X(x: Int)
  case class Q(x: String)

  test("Get should be derived for unary products") {
    Get[X]
    Get[Q]
  }
}
