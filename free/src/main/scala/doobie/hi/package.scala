package doobie

import doobie.free.{ connection => C }
import doobie.free.{ preparedstatement => PS }
import doobie.free.{ callablestatement => CS }
import doobie.free.{ resultset => RS }
import doobie.free.{ statement => S }
import doobie.free.{ databasemetadata => DMD }

/** 
 * High-level database API.
 * @group Modules
 */
package object hi {

  /** @group Aliases */  type ConnectionIO[A]        = C.ConnectionIO[A]
  /** @group Aliases */  type StatementIO[A]         = S.StatementIO[A]
  /** @group Aliases */  type CallableStatementIO[A] = CS.CallableStatementIO[A]
  /** @group Aliases */  type PreparedStatementIO[A] = PS.PreparedStatementIO[A]
  /** @group Aliases */  type DatabaseMetaDataIO[A]  = DMD.DatabaseMetaDataIO[A]

}