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

  protected def loadDriver: Action[Unit] =
    asks(i => Class.forName(i.driverClassName)).void :++> "LOAD DRIVER"

  protected def connection: Action[Connection] = 
    loadDriver >> asks(i => DriverManager.getConnection(i.url, i.user, i.pass))

  // Turns a connection action into a database transaction
  def lift[A](a: conn.Action[A]): Action[A] =
    fops.resource[Connection, A](
      connection :++>> (c => s"OPEN $c"),
      c => gosub(conn.runrw(c, a)),
      c => success(c.close) :++> s"CLOSE $c")

  def run[A](ci: ConnectInfo[_], a: Action[A]): (Log, Throwable \/ A) =
    runrw(ci, a)

}

