// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

class invariantspec extends munit.FunSuite {
  test("NonNullableColumnRead should include a one-based indexing disclaimer") {
    val ex = invariant.NonNullableColumnRead(1, doobie.enum.JdbcType.Array)
    assert(ex.getMessage.contains("is 1-based"))
  }
}
