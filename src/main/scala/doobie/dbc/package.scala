package doobie

import scalaz._
import scalaz.syntax.monad._
import scalaz.syntax.effect.monadCatchIO._
import scalaz.effect._
import scalaz.effect.IO._
import scalaz.effect.kleisliEffect._

package object dbc {

  type Savepoint = java.sql.Savepoint

  type Connection[+A] = connection.Connection[A]  
  type Statement[+A] = statement.Statement[A]
  type DatabaseMetaData[+A] = databasemetadata.DatabaseMetaData[A]
  type CallableStatement[+A] = callablestatement.CallableStatement[A]
  type PreparedStatement[+A] = preparedstatement.PreparedStatement[A]
  type ResultSet[+A] = resultset.ResultSet[A]

  type Log[L] = util.TreeLogger[L]

}

