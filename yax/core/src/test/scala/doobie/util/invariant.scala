package doobie.util

import org.specs2.mutable.Specification

object invariantspec extends Specification {
  "NonNullableColumnRead" >> {
    "include a one-based indexing disclaimer" in {
      val ex = invariant.NonNullableColumnRead(1, doobie.enum.jdbctype.Array)
      ex.getMessage must beMatching(".*is 1-based[.]$")
    }
  }
}
