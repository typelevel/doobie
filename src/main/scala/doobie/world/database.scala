package doobie
package world

import scalaz.effect.IO
import java.sql._
import scalaz._
import Scalaz._

import doobie.util._
import doobie.world.{connection => conn}

object database extends DWorld.Stateless {
  import rwsfops._

  protected type R = ConnectInfo[A] forSome { type A } // we don't care

  // We parameterize the connect info with the driver and require a manifest. This is distasteful
  // but at least it lets us verify at compile-time that the driver class is available (although at 
  // runtime we must load it by name).
  case class ConnectInfo[D](url: String, user: String, pass: String)(implicit mf: Manifest[D]) {
    def driverClassName: String = mf.runtimeClass.getName
  }

  /** Ensure the driver is loaded. */
  private def loadDriver: Action[Unit] =
    asks(i => Class.forName(i.driverClassName)).void :++> "LOAD DRIVER"

  /** Open a connecton. */
  private def connection: Action[Connection] = 
    loadDriver >> asks(i => DriverManager.getConnection(i.url, i.user, i.pass)) :++>> (c => s"OPEN $c")

  /** Close a connection. */
  private def close(c: Connection): Action[Unit] =
    unit(c.close) :++> s"CLOSE $c"

  /** Pass a new connection to the given continuation. */
  private[world] def connect[A](k: Connection => (W, Throwable \/ A)): Action[A] =
    fops.resource[Connection, A](connection, c => gosub(k(c)), close)

  implicit class DatabaseOps[A](a: Action[A]) {

    /** Given connection information, lift this action into IO. */
    def lift(ci: ConnectInfo[_]): IO[(Log, Throwable \/ A)] =
      IO(runrw(ci, a))

  }

}

