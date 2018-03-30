// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

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
    val  HikariTransactor       = hikari.HikariTransactor
  }

}
