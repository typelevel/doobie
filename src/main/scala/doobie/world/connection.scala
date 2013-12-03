package doobie
package world

import java.sql._
import scalaz._
import scalaz.effect.IO
import Scalaz._
import doobie.util._

import doobie.world.{ statement => stmt }

object connection extends DWorld.Stateless {

  protected type R = Connection

  def rollback: Action[Unit] =
    asks(_.rollback) :++> "ROLLBACK"

  def commit: Action[Unit] =
    asks(_.commit) :++> "COMMIT"

  def prepare[A](sql: String, f: PreparedStatement => (W, Throwable \/ A)): Action[A] =
    fops.resource[PreparedStatement, A](
      asks(_.prepareStatement(sql)) :++>> (ps => s"PREPARE $ps"),
      ps => gosub(f(ps)),
      ps => success(ps.close) :++> s"CLOSE $ps")

  implicit class ConnectionActionOps[A](a: Action[A]) {
    def lift(ci: database.ConnectInfo[_]): IO[(W, Throwable \/ A)] =
      database.connect(runrw(_, a)).lift(ci)
  }

}

