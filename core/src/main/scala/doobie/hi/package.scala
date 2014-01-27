package doobie

import java.sql
import dbc._
import scalaz._
import Scalaz._
import scalaz.effect.{IO, MonadIO}
import scalaz.effect.kleisliEffect._
import scalaz.syntax.effect.monadCatchIO._
import scalaz.stream._

/** Pure functional high-level JDBC layer. */
package object hi extends co.PreparedStatementCombinators[sql.PreparedStatement] with co.ResultSetCombinators {

 



  implicit class SqlInterpolator(val sc: StringContext) {
    import connection.prepareStatement

    class Source[A: Comp](a: A) {

      def go[B](b: PreparedStatement[B]): Connection[B] =
        prepareStatement(sc.parts.mkString("?"))(set(1, a) >> b)

      def executeQuery[B](b: ResultSet[B]): Connection[B] =
        go(preparedstatement.executeQuery(b))
    
      def execute: Connection[Boolean] =
        go(preparedstatement.execute)

      def executeUpdate: Connection[Int] =
        go(preparedstatement.executeUpdate)

    }


    class Source0 extends Source[Int](1) { // TODO: fix this

      override def go[B](b: PreparedStatement[B]): Connection[B] =
        prepareStatement(sc.parts.mkString("?"))(b)

    }

    def sql() = new Source0

    def sql[A: Prim](a: A) = new Source(a)

    def sql[A: Prim, B: Prim](a: A, b: B) = new Source((a,b))

    def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim](a: A, b: B, c: C, d: D, e: E) = new Source((a,b,c,d,e))
    def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim, F: Prim](a: A, b: B, c: C, d: D, e: E, f: F) = new Source((a,b,c,d,e, f))

  }
  
}