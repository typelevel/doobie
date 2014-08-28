package doobie.util

import doobie.free.connection.ConnectionIO
import doobie.util.invariant.MappingViolation

object prepared {

  trait Prepared {
    def sql: String
    def check: ConnectionIO[List[MappingViolation]]
  }

}