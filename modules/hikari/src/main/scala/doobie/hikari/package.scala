// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie

import com.zaxxer.hikari.HikariDataSource

package object hikari {

  type HikariTransactor[M[_]] = Transactor.Aux[M, HikariDataSource]

}
