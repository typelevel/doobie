package doobie
package world

import java.sql._
import scalaz._
import Scalaz._
import doobie.util.RWSFWorld

object ConnectionWorld extends DWorld {

  protected type R = Connection
  protected type S = Unit

  def statement[A](sql: String, a: StatementWorld.Action[A]): Action[A] = 
    fops.resource[PreparedStatement, A](
      asks(_.prepareStatement(sql)) :++>> (ps => s"PREPARE $ps"),
      ps => gosub(StatementWorld.runi(ps, a)),
      ps => success(ps.close) :++> s"DISPOSE $ps")

  def rollback: Action[Unit] =
    asks(_.rollback) :++> "ROLLBACK"

  def commit: Action[Unit] =
    asks(_.commit) :++> "COMMIT"

  def runc[A](c: Connection, a: Action[A]): (W, Throwable \/ A) =
    runrws(c, (), a) match {
      case (w, _, e) => (w, e)
    }

}