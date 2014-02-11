package doobie

import java.sql
import scalaz._
import Scalaz._
import scalaz.effect.{IO, MonadIO}
import scalaz.effect.KleisliEffectInstances
import scalaz.syntax.effect.monadCatchIO._
import scalaz.stream._

/** Pure functional high-level JDBC layer. */
package object hi extends KleisliEffectInstances with ToCatchSqlOps {

  // Modules
  object preparedstatement extends  co.PreparedStatementCombinators[sql.PreparedStatement]
  object resultset extends co.ResultSetCombinators
  object connection extends co.ConnectionCombinators

  type Connection[+A]        = connection.Action[A]  
  // type Statement[+A]         = statement.Action[A]
  // type DatabaseMetaData[+A]  = databasemetadata.Action[A]
  // type CallableStatement[+A] = callablestatement.Action[A]
  // type ParameterMetaData[+A] = parametermetadata.Action[A]
  type PreparedStatement[+A] = preparedstatement.Action[A]
  type ResultSet[+A]         = resultset.Action[A]
  // type ResultSetMetaData[+A] = resultsetmetadata.Action[A]

  type DBIO[+A] = Connection[A]

  type Action0[S0, +A] = dbc.Action0[S0, A]

  implicit def catchableAction0[S]: Catchable[({ type l[a] = Action0[S, a] })#l] =
    dbc.catchableAction0[S]


  implicit class DBIOSyntax[A](a: DBIO[A]) {
    def run(t: Transactor): IO[A] =
      t.exec(a)
  }



  implicit class ProcessOps[F[+_]: Monad: Catchable, A](fa: Process[F,A]) {

    def toVector: F[Vector[A]] =
      fa.runLog.map(_.toVector)

    def toList: F[List[A]] =
      fa.runLog.map(_.toList)

    def sink(f: A => IO[Unit])(implicit ev: MonadIO[F]): F[Unit] =     
      fa.to(Process.repeatEval(((a: A) => f(a).liftIO[F]).point[F])).run

  }


  implicit class SqlInterpolator(val sc: StringContext) {
    import connection.prepareStatement

    class Source[A: Comp](a: A) {

      def go[B](b: PreparedStatement[B]): Connection[B] =
        prepareStatement(sc.parts.mkString("?"))(preparedstatement.set(1, a) >> b)

      def executeQuery[B](b: ResultSet[B]): Connection[B] =
        go(preparedstatement.executeQuery(b))
    
      def execute: Connection[Boolean] =
        go(preparedstatement.execute)

      def executeUpdate: Connection[Int] =
        go(preparedstatement.executeUpdate)

      def process[O: Comp]: Process[Connection, O] =
        connection.process[O](sc.parts.mkString("?"), preparedstatement.set1(a))

    }

    class Source0 extends Source[Int](1) { // TODO: fix this

      override def go[B](b: PreparedStatement[B]): Connection[B] =
        prepareStatement(sc.parts.mkString("?"))(b)

      override def process[O: Comp]: Process[Connection, O] =
        connection.process[O](sc.parts.mkString("?"),().point[preparedstatement.Action])

    }

    def sql() = new Source0
    def sql[A: Prim](a: A) = new Source(a)
    def sql[A: Prim, B: Prim](a: A, b: B) = new Source((a,b))
    def sql[A: Prim, B: Prim, C: Prim](a: A, b: B, c: C) = new Source((a,b,c))
    def sql[A: Prim, B: Prim, C: Prim, D: Prim](a: A, b: B, c: C, d: D) = new Source((a,b,c,d))
    def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim](a: A, b: B, c: C, d: D, e: E) = new Source((a,b,c,d,e))
    def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim, F: Prim](a: A, b: B, c: C, d: D, e: E, f: F) = new Source((a,b,c,d,e, f))

  }
  
}