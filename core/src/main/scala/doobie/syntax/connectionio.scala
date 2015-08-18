package doobie.syntax

import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import doobie.util.connector.Connector

object connectionio {

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


  /** Syntax that adds `Transactor` operations to `ConnectionIO`. */
  implicit class ConnectionIOTransactorOps[A](ma: ConnectionIO[A]) {

    final class Helper[M[_]] {
      def apply[T](t: T)(implicit ev1: Transactor[M, T]): M[A] =
        Transactor.safeTrans(t).apply(ma)
    }

    /** 
     * Interpret this program configured with transaction logic as defined by the `Transactor`'s
     * `LiftXA`, into effect-capturing target monad `M`, running on a dedicated `Connection`. 
     */
    def transact[M[_]] = new Helper[M]

  }

}
