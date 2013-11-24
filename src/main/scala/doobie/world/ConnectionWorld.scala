package doobie
package world

import java.sql._
import scalaz._
import Scalaz._
import doobie.util.RWSFWorld

object ConnectionWorld extends RWSFWorld[Connection, Log, Unit] {

  def statement[A](sql: String, a: StatementWorld.Action[A]): Action[A] = {

    def acquire: Action[PreparedStatement] = 
      asks(_.prepareStatement(sql)) :++>> (ps => s"PREPARE $ps")

    def use(ps: PreparedStatement): Action[A] = 
      success(a.unsafeRun(ps)) >>= { // TODO: factor out nested world call
        case (s, e) => tell(s.w) >> e.fold(fail(_), success(_))
      }

    def dispose(ps: PreparedStatement): Action[Unit] = 
      success(ps.close) :++> s"DISPOSE $ps"

    resource(acquire)(use)(dispose)

  }

  def rollback: Action[Unit] =
    asks(_.rollback) :++> "ROLLBACK"

  def commit: Action[Unit] =
    asks(_.commit) :++> "COMMIT"

  implicit class RunnableAction[A](a: Action[A]) {
    def unsafeRun(c: Connection) = 
      run(State(c, Vector(), ()), a)
  }

}