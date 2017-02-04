package doobie.syntax

import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor

#+scalaz
import scalaz.Monad
#-scalaz
#+cats
import cats.Monad
#-cats

object connectionio {

  implicit class MoreConnectionIOOps[A](ma: ConnectionIO[A]) {
    def transact[M[_]: Monad](xa: Transactor[M, _]): M[A] =
      xa.trans.apply(ma)
  }

}
