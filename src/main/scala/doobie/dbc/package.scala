package doobie

import scalaz._
import scalaz.syntax.monad._
import scalaz.syntax.effect.monadCatchIO._
import scalaz.effect._
import scalaz.effect.IO._
import scalaz.effect.kleisliEffect._

package object dbc {

  object callablestatement extends CallableStatementFunctions
  object connection        extends ConnectionFunctions
  object databasemetadata  extends DatabaseMetaDataFunctions
  object parametermetadata extends ParameterMetaDataFunctions
  object preparedstatement extends PreparedStatementFunctions
  object resultset         extends ResultSetFunctions
  object resultsetmetadata extends ResultSetMetaDataFunctions
  object statement         extends StatementFunctions

  type Connection[+A]        = connection.Action[A]  
  type Statement[+A]         = statement.Action[A]
  type DatabaseMetaData[+A]  = databasemetadata.Action[A]
  type CallableStatement[+A] = callablestatement.Action[A]
  type ParameterMetaData[+A] = parametermetadata.Action[A]
  type PreparedStatement[+A] = preparedstatement.Action[A]
  type ResultSet[+A]         = resultset.Action[A]
  type ResultSetMetaData[+A] = resultsetmetadata.Action[A]

  type Log[L] = util.TreeLogger[L]

}

