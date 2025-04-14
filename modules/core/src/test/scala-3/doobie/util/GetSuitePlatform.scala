// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import Predef.augmentString
import doobie.util.invariant.InvalidValue

enum EnumWithOnlySingletonCases {
  case One, Two, Three
}

trait GetSuitePlatform { self: munit.FunSuite =>

  enum EnumContainsNonSinletonCase {
    case One
    case Two(i: Int)
  }

  sealed trait NotEnum

  case object NotEnum1 extends NotEnum

  test("Get should not be derived for enum with non singleton case") {
    val compileError = compileErrors("Get.deriveEnumString[EnumContainsNonSinletonCase]")

    assert(compileError.contains("Enum EnumContainsNonSinletonCase contains non singleton case Two"))
  }

  test("Get should be derived for enum with only singleton cases") {
    Get.deriveEnumString[EnumWithOnlySingletonCases]
  }

  test("Get should not be derived for sealed trait") {
    val compileError = compileErrors("Get.deriveEnumString[NotEnum]")

    assert(compileError.contains(
      "Type argument GetSuitePlatform.this.NotEnum does not conform to upper bound scala.reflect.Enum"))
  }
}

trait GetDBSuitePlatform { self: munit.CatsEffectSuite & TransactorProvider =>
  import doobie.syntax.all.*

  given Get[EnumWithOnlySingletonCases] = Get.deriveEnumString

  test("Get should properly read existing value of enum") {
    sql"select 'One'".query[EnumWithOnlySingletonCases].unique.transact(xa).attempt.assertEquals(
      Right(
        EnumWithOnlySingletonCases.One
      )
    )
  }

  test("Get should error reading non existing value of enum") {
    sql"select 'One1'".query[EnumWithOnlySingletonCases].unique.transact(xa).attempt.assertEquals(
      Left(
        InvalidValue(
          value = "One1",
          reason =
            "enum EnumWithOnlySingletonCases does not contain case: One1"
        )
      )
    )
  }
}
