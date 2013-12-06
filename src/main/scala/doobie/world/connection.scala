package doobie
package world

import java.sql._
import scalaz._
import scalaz.effect.IO
import Scalaz._
import doobie.util._

import doobie.world.{ statement => stmt }

object connection extends DWorld.Stateless {
  import rwsfops._

  protected type R = Connection

  /** Roll back the current transaction. */
  def rollback: Action[Unit] =
    asks(_.rollback) :++> "ROLLBACK"

  /** Commit the current transaction. */
  def commit: Action[Unit] =
    asks(_.commit) :++> "COMMIT"

  /** Prepare a statement. */
  private def prepare(sql: String): Action[PreparedStatement] =
    asks(_.prepareStatement(sql)) :++>> (ps => s"PREPARE $ps")

  /** Closes a statement. */
  private def close(ps: PreparedStatement): Action[Unit] =
    unit(ps.close) :++> s"CLOSE $ps"

  /** Prepare a statement and pass it to the provided continuation. */
  private[world] def prepare[A](sql: String, f: PreparedStatement => (W, Throwable \/ A)): Action[A] =
    fops.resource[PreparedStatement, A](prepare(sql), ps => gosub(f(ps)), close)

  implicit class ConnectionActionOps[A](a: Action[A]) {

    /** Lift this action into database world. */
    def lift: database.Action[A] =
      database.connect(runrw(_, a))
  
  }

}

