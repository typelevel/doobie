package doobie

import java.sql
import scalaz._
import scalaz.syntax.monad._
import scalaz.syntax.effect.monadCatchIO._
import scalaz.effect._
import scalaz.effect.IO._
import scalaz.effect.kleisliEffect._
import scala.annotation.unchecked.uncheckedVariance

/** Pure functional low-level JDBC layer. */
package object dbc {

  object callablestatement extends op.CallableStatementOps
  object connection extends op.ConnectionOps
  object databasemetadata extends op.DatabaseMetaDataOps
  object parametermetadata extends op.ParameterMetaDataOps
  object preparedstatement extends op.PreparedStatementOps[sql.PreparedStatement]
  object resultset extends op.ResultSetOps
  object resultsetmetadata extends op.ResultSetMetaDataOps
  object statement extends op.StatementOps[sql.Statement]

  type Connection[+A]        = connection.Action[A]  
  type Statement[+A]         = statement.Action[A]
  type DatabaseMetaData[+A]  = databasemetadata.Action[A]
  type CallableStatement[+A] = callablestatement.Action[A]
  type ParameterMetaData[+A] = parametermetadata.Action[A]
  type PreparedStatement[+A] = preparedstatement.Action[A]
  type ResultSet[+A]         = resultset.Action[A]
  type ResultSetMetaData[+A] = resultsetmetadata.Action[A]

  type Log[L] = util.TreeLogger[L]
  type Action0[S0, +A] = Kleisli[IO, (Log[LogElement], S0), A]

  implicit def catchableAction0[S]: Catchable[({ type l[a] = Action0[S, a] })#l] =
    new Catchable[({ type l[a] = Action0[S, a] })#l] {
      def attempt[A](fa: Action0[S,A]) = fa.map(a => \/.fromTryCatch(a))
      def fail[A](t: Throwable) = Kleisli(_ => IO(throw t))
    }

  catchableAction0[sql.Connection] : Catchable[Connection]

}

