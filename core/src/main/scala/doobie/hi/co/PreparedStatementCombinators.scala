package doobie
package hi
package co

import java.sql
import dbc.op.PreparedStatementOps
import scalaz._
import Scalaz._
import scalaz.effect.{IO, MonadIO}
import scalaz.effect.kleisliEffect._
import scalaz.syntax.effect.monadCatchIO._
import scalaz.stream._

trait PreparedStatementCombinators[A <: sql.PreparedStatement] extends PreparedStatementOps[A] {

  def set[A](index: Int, a: A)(implicit A: Comp[A]): Action[Unit] =
    push(s"structured set at index $index: $a")(A.set(index, a))
  
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

