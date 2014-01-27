package doobie
package hi
package co

import java.sql
import dbc._
import dbc.op._
import scalaz._
import Scalaz._
import scalaz.effect.{IO, MonadIO}
import scalaz.effect.kleisliEffect._
import scalaz.syntax.effect.monadCatchIO._
import scalaz.stream._

trait PreparedStatementCombinators[A <: sql.PreparedStatement] extends PreparedStatementOps[A] {

  def set[A](index: Int, a: A)(implicit A: Comp[A]): Action[Unit] =
    push(s"structured set at index $index: $a")(A.set(index, a))
  
}