package doobie

import org.h2.jdbcx.JdbcConnectionPool

package object h2 {

  type H2Transactor[M[_]] = Transactor.Aux[M, JdbcConnectionPool]

  object implicits
    extends Metas
       with syntax.ToH2TransactorOps

  @deprecated(message = "import doobie.h2._, doobie.h2.implicits._", since = "0.5.0")
  object imports
    extends Metas
       with syntax.ToH2TransactorOps {
    type H2Transactor[M[_]] = h2.H2Transactor[M]
  }

}
