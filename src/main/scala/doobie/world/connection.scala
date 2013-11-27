package doobie
package world

import java.sql._
import scalaz._
import Scalaz._
import doobie.util._

import doobie.world.{ statement => stmt }

object connection extends DWorld.Stateless {

  protected type R = Connection

  // Construct an action that prepares a statement and feeds it to the given action
  def statement[A](sql: String, a: stmt.Action[A]): Action[A] = 
    fops.resource[PreparedStatement, A](
      asks(_.prepareStatement(sql)) :++>> (ps => s"PREPARE $ps"),
      ps => gosub(stmt.runi(ps, a)),
      ps => success(ps.close) :++> s"DISPOSE $ps")

  def rollback: Action[Unit] =
    asks(_.rollback) :++> "ROLLBACK"

  def commit: Action[Unit] =
    asks(_.commit) :++> "COMMIT"

}