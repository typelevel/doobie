package doobie
package world

import scalaz.effect.IO
import java.sql._
import scalaz._
import Scalaz._

import doobie.util._
import doobie.world.{connection => conn}

object database extends DWorld.Stateless {

  // We parameterize the connect info with the driver and require a manifest. This is distasteful
  // but at least it lets us verify at compile-time that the driver class is available (although at 
  // runtime we must load it by name).
  case class ConnectInfo[D](url: String, user: String, pass: String)(implicit mf: Manifest[D]) {
    def driverClassName: String = mf.runtimeClass.getName
  }

  protected type R = ConnectInfo[_]

  ////// ACTIONS

  private def loadDriver: Action[Unit] =
    asks(i => Class.forName(i.driverClassName)).void :++> "LOAD DRIVER"

  private def connection: Action[Connection] = 
    loadDriver >> asks(i => DriverManager.getConnection(i.url, i.user, i.pass))

  ////// COMBINATORS

  def connect[A](k: Connection => (W, Throwable \/ A)): Action[A] =
    fops.resource[Connection, A](
      connection :++>> (c => s"OPEN $c"),
      c => gosub(k(c)),
      c => success(c.close) :++> s"CLOSE $c")

  // LIFTING INTO IO

  def lift[A](ci: ConnectInfo[_], a: Action[A]): IO[(Log, Throwable \/ A)] =
    IO(runrw(ci, a))

  // SYNTAX

  implicit class DatabaseOps[A](a: Action[A]) {
    def lift(ci: ConnectInfo[_]): IO[(Log, Throwable \/ A)] =
      database.lift(ci, a)
  }

}

