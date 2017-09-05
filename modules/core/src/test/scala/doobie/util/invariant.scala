// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import org.specs2.mutable.Specification

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object invariantspec extends Specification {
  "NonNullableColumnRead" >> {
    "include a one-based indexing disclaimer" in {
      val ex = invariant.NonNullableColumnRead(1, doobie.enum.JdbcType.Array)
      ex.getMessage must beMatching(".*is 1-based[.]$")
    }
  }
}
