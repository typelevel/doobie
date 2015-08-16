package doobie.syntax

import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor

object connectionio {

  // implicit class MoreConnectionIOOps[A](ma: ConnectionIO[A]) {
  //   def transact[M[_]](xa: Transactor[M]): M[A] =
  //     xa.trans(ma)
  // }

}
