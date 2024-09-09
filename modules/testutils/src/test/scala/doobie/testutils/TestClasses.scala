// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.testutils

// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

// Class definitions for testing purposes. (e.g. derivation)
object TestClasses {
  case class CCInt(x: Int)

  case class CCString(x: String)

  case class CCIntString(i: Int, s: String)

  case class CCAnyVal(s: String) extends AnyVal

  object PlainObj
}
