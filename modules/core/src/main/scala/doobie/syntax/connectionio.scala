package doobie.syntax

import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor

import scalaz.Monad

object connectionio {

  implicit class MoreConnectionIOOps[A](ma: ConnectionIO[A]) {
    def transact[M[_]: Monad](xa: Transactor[M]): M[A] =
      xa.trans.apply(ma)
  }

}
