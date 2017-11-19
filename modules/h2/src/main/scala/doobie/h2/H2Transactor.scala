// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie
package h2

import cats.effect.Async
import fs2.Stream
import org.h2.jdbcx.JdbcConnectionPool

object H2Transactor {

  /** Constructs a program that constructs a new H2Transactor. */
  @deprecated("This method has been renamed `newH2Transactor` to help clarify usage", "doobie 0.5.0")
  def apply[M[_]: Async](url: String, user: String, pass: String): M[H2Transactor[M]] =
    newH2Transactor(url, user, pass)

  /** Constructs a program that constructs a new H2Transactor. */
  def newH2Transactor[M[_]: Async](url: String, user: String, pass: String): M[H2Transactor[M]] =
    Async[M].delay(Transactor.fromDataSource[M](JdbcConnectionPool.create(url, user, pass)))

  /** Constructs a stream that emits a single `HikariTransactor` with guaranteed cleanup. */
  def stream[M[_]: Async](url: String, user: String, pass: String) : Stream[M, H2Transactor[M]] =
    Stream.bracket(newH2Transactor(url, user, pass))(Stream.emit(_), _.configure(ds => Async[M].delay(ds.dispose())))

}
