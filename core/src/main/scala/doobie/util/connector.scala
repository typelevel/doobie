package doobie.util

import doobie.imports.{ Capture, ConnectionIO, toDoobieCatchableOps, toProcessOps, HC, FC }
import doobie.imports.HC.delay

import doobie.hi.connection.ProcessConnectionIOOps // TODO: add to imports module

import scalaz.{ Monad, Catchable, Kleisli, ~>, @>, |-->, Lens }
import scalaz.syntax.monad._
import scalaz.stream.Process
import scalaz.stream.Process.{ eval, eval_, halt }

import java.sql.Connection

object connector {

  /** 
   * Configuration for a connector; these actions are used by a connector's `safe` method to take
   * an existing `ConnectionIO` and equip it to execute as a transaction.
   */ 
  final case class Config(
    before:  ConnectionIO[Unit],
    oops:    ConnectionIO[Unit],
    after:   ConnectionIO[Unit],
    always:  ConnectionIO[Unit]
  )
  object Config {

    val default = Config(
      FC.setAutoCommit(false),
      FC.rollback,
      FC.commit,
      FC.close
    )

    // Lenses
    val before: Config @> ConnectionIO[Unit] = Lens.lensu((a, b) => a.copy(before = b), _.before)
    val oops:   Config @> ConnectionIO[Unit] = Lens.lensu((a, b) => a.copy(oops   = b), _.oops)
    val after:  Config @> ConnectionIO[Unit] = Lens.lensu((a, b) => a.copy(after  = b), _.after)
    val always: Config @> ConnectionIO[Unit] = Lens.lensu((a, b) => a.copy(always = b), _.always)

  }

  /** 
   * Typeclass for data types that can carry a `Config` and can provide a `Connection` in any given
   * effect-capturing monad.
   */
  trait Connector[T] {
    def config: T @> Config
    def connect[M[_]: Monad: Capture: Catchable](xa: T): M[Connection]
  }

  object Connector {

    // Lenses
    def config[T](implicit ev: Connector[T]) = ev.config
    def before[T: Connector] = config[T] >=> Config.before
    def oops  [T: Connector] = config[T] >=> Config.oops
    def after [T: Connector] = config[T] >=> Config.after
    def always[T: Connector] = config[T] >=> Config.always

    /** Program yielding a Connection in the given monad. */
    def connect[M[_]: Monad: Catchable: Capture, T](t: T)(implicit ev: Connector[T]): M[Connection] =
      ev.connect[M](t)

    /** Wrap a `ConnectionIO` in before/after/oops/always logic. */
    def safe[T: Connector, A](t: T)(ma: ConnectionIO[A]): ConnectionIO[A] =
      (before[T].get(t) *> ma <* after[T].get(t)) onException oops[T].get(t) ensuring always[T].get(t)

    /** Wrap a `Process[ConnectionIO, ?]` in before/after/oops/always logic. */
    def safeP[T: Connector, A](t: T)(pa: Process[ConnectionIO, A]): Process[ConnectionIO, A] =
      (eval_(before[T].get(t)) ++ pa ++ eval_(after[T].get(t))) onFailure { e => 
        eval_(oops[T].get(t)) ++ eval_(delay(throw e)) 
      } onComplete eval_(always[T].get(t))

    /** Natural transformation to target monad `M`. */
    def trans[M[_]: Monad: Catchable: Capture, T: Connector](t: T): (ConnectionIO ~> M) =
      new (ConnectionIO ~> M) {        
        def apply[A](ma: ConnectionIO[A]) = 
          connect(t) >>= ma.transK[M]      
      }

    /** Natural transformation to an equivalent process over target monad `M`. */
    def transP[M[_]: Monad: Catchable: Capture, T: Connector](t: T): (Process[ConnectionIO, ?] ~> Process[M, ?]) =
      new (Process[ConnectionIO, ?] ~> Process[M, ?]) {
        def apply[A](pa: Process[ConnectionIO, A]) = 
          eval(connect(t)) >>= pa.trans[M]    
      }

  }

  /** Syntax for `Connector`s. */
  implicit class ConnectorOps[T: Connector](t: T) {

    // Stores
    def before = Connector.before[T].apply(t)
    def oops   = Connector.oops[T].apply(t)
    def after  = Connector.after[T].apply(t)
    def always = Connector.always[T].apply(t)

    /** Wrap a `ConnectionIO` in before/after/oops/always logic. */
    def safe[A](ma: ConnectionIO[A]): ConnectionIO[A] =
      Connector.safe(t)(ma)

    /** Natural transformation to target monad `M`. */
    def trans[M[_]: Monad: Catchable: Capture]: (ConnectionIO ~> M) = 
      Connector.trans(t)

    /** Natural transformation to an equivalent process over target monad `M`. */
    def transP[M[_]: Monad: Catchable: Capture]: (Process[ConnectionIO, ?] ~> Process[M, ?]) =
      Connector.transP(t)

  }

  /** Syntax that adds `Connector` operations to `ConnectionIO`. */
  implicit class ConnectionIOConnectorOps[A](ma: ConnectionIO[A]) {

    final class Helper[M[_]](safe: Boolean) {
      def apply[T: Connector](t: T)(implicit ev1: Monad[M], ev2: Capture[M], ev3: Catchable[M]) =
        t.trans.apply(if (safe) t.safe(ma) else ma)
    }

    /** 
     * Interpret this program unmodified, into effect-capturing target monad `M`, running on a 
     * dedicated `Connection`.
     */
    def connect[M[_]]  = new Helper[M](false)

    /** 
     * Interpret this program configured with transaction logic as defined by the connector's
     * `Config`, into effect-capturing target monad `M`, running on a dedicated `Connection`. 
     */
    def transact[M[_]] = new Helper[M](true)

  }



  // Driver manager

  import doobie.free.{ drivermanager => HDM }
  import HDM.DriverManagerIO

  case class DriverManagerConnector(config: Config, connect: DriverManagerIO[Connection])

  object DriverManagerConnector {

    implicit object Instance extends Connector[DriverManagerConnector] {
      def config: DriverManagerConnector @> Config = 
        Lens.lensu((a, b) => a.copy(config = b), _.config)
      def connect[M[_]: Monad: Capture: Catchable](xa: DriverManagerConnector): M[Connection] =
        xa.connect.trans[M]
    }

    private def create(driver: String, conn: DriverManagerIO[Connection]): DriverManagerConnector =
      DriverManagerConnector(Config.default, HDM.delay(Class.forName(driver)) *> conn)

    def apply(driver: String, url: String): DriverManagerConnector =
      create(driver, HDM.getConnection(url))

    def apply(driver: String, url: String, user: String, pass: String): DriverManagerConnector =
      create(driver, HDM.getConnection(url, user, pass))

    def apply(driver: String, url: String, info: java.util.Properties): DriverManagerConnector =
      create(driver, HDM.getConnection(url, info))

  }



}


