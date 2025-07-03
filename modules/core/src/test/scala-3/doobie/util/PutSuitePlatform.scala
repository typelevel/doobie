// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import Predef.augmentString

trait PutSuitePlatform { self: munit.FunSuite =>

  enum EnumContainsNonSinletonCase {
    case One
    case Two(i: Int)
  }

  enum EnumWithOnlySingletonCases {
    case One, Two, Three
  }

  sealed trait NotEnum

  case object NotEnum1 extends NotEnum

  test("Put should not be derived for enum with non singleton case") {
    val compileError = compileErrors("Put.deriveEnumString[EnumContainsNonSinletonCase]")

    assert(compileError.contains("Enum EnumContainsNonSinletonCase contains non singleton case Two"))
  }

  test("Put should be derived for enum with only singleton cases") {
    Put.deriveEnumString[EnumWithOnlySingletonCases]
  }

  test("Put should not be derived for sealed trait") {
    val compileError = compileErrors("Put.deriveEnumString[NotEnum]")

    assert(compileError.contains(
      "Type argument PutSuitePlatform.this.NotEnum does not conform to upper bound scala.reflect.Enum"))
  }
}
