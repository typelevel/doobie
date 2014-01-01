package doobie

import scalaz._
import scalaz.effect.IO
import scalaz.effect.IO._
import scalaz.effect.kleisliEffect._
import Kleisli._

package object dbc {

  type Savepoint = java.sql.Savepoint

  type Connection[+A] = connection.Connection[A]  
  type Statement[+A] = statement.Statement[A]
  type DatabaseMetaData[+A] = databasemetadata.DatabaseMetaData[A]
  type CallableStatement[+A] = callablestatement.CallableStatement[A]
  type PreparedStatement[+A] = preparedstatement.PreparedStatement[A]
  type ResultSet[+A] = resultset.ResultSet[A]

}