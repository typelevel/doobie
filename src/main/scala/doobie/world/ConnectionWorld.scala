package doobie
package world

import java.sql._
import scalaz._
import Scalaz._

object ConnectionWorld extends ReaderWriterStateFailWorld {

  type R = Connection
  type S = Unit

  def effect[A](f: R => A): Action[A] =
    ask.map(f)

  def statement[A](sql: String, a: StatementWorld.Action[A]): Action[A] = {

    def acquire: Action[PreparedStatement] = 
      effect(_.prepareStatement(sql)) :++>> (ps => s"PREPARE $ps")

    def use(ps: PreparedStatement): Action[A] = 
      success(a.unsafeRun(ps)) >>= { // TODO: factor out nested world call
        case (s, e) => tell(s.w) >> e.fold(fail(_), success(_))
      }

    def dispose(ps: PreparedStatement): Action[Unit] = 
      success(ps.close) :++> s"DISPOSE $ps"

    resource(acquire)(use)(dispose)

  }

  def rollback: Action[Unit] =
    effect(_.rollback) :++> "ROLLBACK"

  def commit: Action[Unit] =
    effect(_.commit) :++> "COMMIT"

  implicit class RunnableAction[A](a: Action[A]) {
    def unsafeRun(c: Connection) = 
      run(State(c, Vector(), ()), a)
  }

}