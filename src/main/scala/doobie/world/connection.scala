package doobie
package world

import java.sql._
import scalaz._
import scalaz.effect._
import Scalaz._
import doobie.util._

import doobie.world.{ statement => stmt }

object connection extends RWSFWorld with EventLogging with UnitState {
  import rwsfops._

  protected type R = Connection

  sealed trait Event
  object Event {
    case class PrepareStatement(sql: String) extends Event {
      override def toString = {
        val fixed = sql.lines.map(_.trim).filterNot(_.isEmpty).mkString(" \\ ")
        s"$productPrefix($fixed)"
      }
    }
    case object CloseStatement extends Event
    case object Rollback extends Event
    case object Commit extends Event
    case class StatementLog(log: stmt.Log) extends Event
  }

  /** Roll back the current transaction. */
  def rollback: Action[Unit] =
    asks(_.rollback) :++> Event.Rollback

  /** Commit the current transaction. */
  def commit: Action[Unit] =
    asks(_.commit) :++> Event.Commit

  /** Prepare a statement. */
  private def prepare(sql: String): Action[PreparedStatement] =
    asks(_.prepareStatement(sql)) :++>> (ps => Event.PrepareStatement(sql))

  /** Closes a statement. */
  private def close(ps: PreparedStatement): Action[Unit] =
    unit(ps.close) :++> Event.CloseStatement

  /** Prepare a statement and pass it to the provided continuation. */
  private[world] def prepare[A](sql: String, f: PreparedStatement => (stmt.Log, Throwable \/ A)): Action[A] =
    fops.resource[PreparedStatement, A](prepare(sql), ps => gosub[stmt.Log, A](f(ps), l => Vector(Event.StatementLog(l))), close)

  implicit class ConnectionActionOps[A](a: Action[A]) {

    /** Lift this action into database world. */
    def run: database.Action[A] =
      database.connect(runrw(_, a))
  
  }

}

