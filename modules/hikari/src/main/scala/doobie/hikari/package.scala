package doobie

import com.zaxxer.hikari.HikariDataSource

package object hikari {

  type HikariTransactor[M[_]] = Transactor.Aux[M, HikariDataSource]

  object implicits
    extends syntax.ToHikariTransactorOps

  @deprecated(message = "import doobie.hikari._, doobie.hikari.implicits._", since = "0.5.0")
  object imports
    extends syntax.ToHikariTransactorOps {
    type HikariTransactor[M[_]] = hikari.HikariTransactor[M]
  }

}
