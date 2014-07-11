package doobie
package hi
package co

import java.sql
import scalaz._
import Scalaz._
import scalaz.effect.IO
import scalaz.stream._

trait PreparedStatementCombinators //[A <: sql.PreparedStatement] 
  extends dbc.op.PreparedStatementOps[sql.PreparedStatement] 
  with ProcessPrimitives[sql.PreparedStatement] {

  def set[A](index: Int, a: A)(implicit A: Comp[A]): Action[Unit] =
    push(s"structured set at index $index: $a")(A.set(index, a))

  def set1[A: Comp](a: A): Action[Unit] =
    set(1, a)

  ////// PROCESS LIFTING

  def process[A: Comp]: Process[Action, A] = 
    resource[sql.ResultSet, A](
      primitive("acqiure", _.executeQuery))(rs =>
      gosub0(rs.point[Action], resultset.close))(rs =>
      gosub0(rs.point[Action], resultset.getNext[A]))
  
  ////// CONVENIENCE COMBINATORS

  /** Consume and return all rows as a Vector. */
  def vector[A: Comp]: Action[Vector[A]] =
    push("vector")(executeQuery(resultset.vector[A]))

  /** Consume and return all rows as a List. */
  def list[A: Comp]: Action[List[A]] = 
    push("list")(executeQuery(resultset.list[A]))

  /** Consume all rows by passing to effectful action `effect`. */
  def sink[A: Comp](effect: A => IO[Unit]): Action[Unit] = 
    push("list")(executeQuery(resultset.sink[A](effect)))

}

