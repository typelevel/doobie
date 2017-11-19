// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie
package h2

import cats.effect.Async
import org.h2.jdbcx.JdbcConnectionPool

object H2Transactor {

  @deprecated("This method has been renamed `newH2Transactor` to help clarify usage", "doobie 0.5.0")
  def apply[M[_]: Async](url: String, user: String, pass: String): M[H2Transactor[M]] =
    newH2Transactor(url, user, pass)

  def newH2Transactor[M[_]: Async](url: String, user: String, pass: String): M[H2Transactor[M]] =
    Async[M].delay(Transactor.fromDataSource[M](JdbcConnectionPool.create(url, user, pass)))

}
