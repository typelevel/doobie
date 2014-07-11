package doobie

import java.sql
import scalaz._
import Scalaz._
import scalaz.effect.{IO, MonadIO}
import scalaz.syntax.effect.monadCatchIO._
import scalaz.stream._
import Process._
import doobie.hi.syntax._

/** Pure functional high-level JDBC layer. */
package object hi extends doobie.util.ToMonadCatchSqlOps {

  // Modules
  object preparedstatement extends  co.PreparedStatementCombinators //[sql.PreparedStatement]
  object resultset extends co.ResultSetCombinators
  object connection extends co.ConnectionCombinators

  type Connection[A]        = connection.Action[A]  
  // type Statement[A]         = statement.Action[A]
  // type DatabaseMetaData[A]  = databasemetadata.Action[A]
  // type CallableStatement[A] = callablestatement.Action[A]
  // type ParameterMetaData[A] = parametermetadata.Action[A]
  type PreparedStatement[A] = preparedstatement.Action[A]
  type ResultSet[A]         = resultset.Action[A]
  // type ResultSetMetaData[A] = resultsetmetadata.Action[A]

  type DBIO[A] = Connection[A]

  // type Action0[S0, A] = dbc.Action0[S0, A]

  // implicit def catchableAction0[S]: Catchable[({ type l[a] = Action0[S, a] })#l] =
  //   dbc.catchableAction0[S]


  implicit class DBIOSyntax[A](a: DBIO[A]) {
    def run(t: Transactor): IO[A] =
      t.exec(a)
  }



  implicit class ProcessOps[F[_]: Monad: Catchable, A](fa: Process[F,A]) {

    def toVector: F[Vector[A]] =
      fa.runLog.map(_.toVector)

    def toList: F[List[A]] =
      fa.runLog.map(_.toList)

    def sink(f: A => IO[Unit])(implicit ev: MonadIO[F]): F[Unit] =     
      fa.to(Process.repeatEval(((a: A) => f(a).liftIO[F]).point[F])).run

  }


  implicit def toSqlInterpolator(sc: StringContext): SqlInterpolator = 
    new SqlInterpolator(sc)

}