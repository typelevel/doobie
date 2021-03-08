// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import doobie.Put

trait PutSuitePlatform { self: PutSuite =>
  case class X(x: Int)
  case class Q(x: String)

  test("Put should be derived for unary products") {
    Put[X]
    Put[Q]
  }
}
