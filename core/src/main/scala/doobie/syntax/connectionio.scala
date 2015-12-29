package doobie.syntax

import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import doobie.util.trace.Trace

object connectionio {

  implicit class MoreConnectionIOOps[A](ma: ConnectionIO[A]) {

    def transact[M[_]](xa: Transactor[M]): M[A] =
      xa.trans(ma)
    
    def transactL[M[_]](xa: Transactor[M], t: Trace[M]): M[A] =
      xa.transL(t)(ma)

  }

}
