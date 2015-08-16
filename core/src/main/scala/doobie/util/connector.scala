package doobie.util

import doobie.util.capture.Capture
import doobie.free.connection.ConnectionIO
import doobie.hi.connection.ProcessConnectionIOOps

import scalaz.{ Monad, Catchable, ~> }
import scalaz.syntax.monad._
import scalaz.stream.Process
import scalaz.stream.Process.eval

import java.sql.Connection

object connector {

  /** 
   * Typeclass for data types that can provide `Connection`s in some context `M`, giving rise to
   * natural transformations from `ConnectionIO` and its `Process` type to their twins in `M`.
   */
  trait Connector[M[_], A] {
    def connect(a:A): M[Connection]
    def trans(a: A): ConnectionIO ~> M
    def transP(a: A): Process[ConnectionIO, ?] ~> Process[M, ?]
  }

  object Connector {

    /** Program yielding a Connection in the given effect-capturing monad. */
    def connect[M[_], T](t: T)(implicit ev: Connector[M, T]): M[Connection] =
      ev.connect(t)

    /** Natural transformation to target monad `M`. */
    def trans[M[_], T](t: T)(implicit ev: Connector[M, T]): (ConnectionIO ~> M) =
      ev.trans(t)

    /** Natural transformation to an equivalent process over target monad `M`. */
    def transP[M[_], T](t: T)(implicit ev: Connector[M, T]): (Process[ConnectionIO, ?] ~> Process[M, ?]) =
      ev.transP(t)

    /** Construct a default instance, given some constraints on M. */
    def instance[M[_]: Monad: Capture: Catchable, A](f: A => M[Connection]): Connector[M, A] =
      new Connector[M, A] {
        def connect(a: A): M[Connection] = f(a)
        def trans(a: A): ConnectionIO ~> M =
          new (ConnectionIO ~> M) {        
            def apply[B](ma: ConnectionIO[B]) = connect(a) >>= ma.transK[M]      
          }
        def transP(a: A): Process[ConnectionIO, ?] ~> Process[M, ?] =
          new (Process[ConnectionIO, ?] ~> Process[M, ?]) {
            def apply[B](pa: Process[ConnectionIO, B]) = eval(connect(a)) >>= pa.trans[M]    
          }
      }

    /** A raw `DataSource` is a `Connector`. */
    implicit def dataSourceConnector[M[_]: Monad: Capture: Catchable]: Connector[M, javax.sql.DataSource] =
      instance(ds => Capture[M].apply(ds.getConnection))

    /** A raw `Connection` is a `Connector`. */
    implicit def connectionConnector[M[_]: Monad: Capture: Catchable]: Connector[M, Connection] =
      instance(_.point[M])

  }

  implicit class ConnectionIOConnectorOps[A](ma: ConnectionIO[A]) {

    class Helper[M[_]] {
      def apply[T](t: T)(implicit ev1: Connector[M, T]): M[A] =
        Connector.trans(t).apply(ma)
    }

    /** 
     * Interpret this program unmodified, into effect-capturing target monad `M`, running on a 
     * dedicated `Connection`.
     */
    def connect[M[_]] = new Helper[M]

  }

}


