package doobie
package h2

import cats.effect.Async
import org.h2.jdbcx.JdbcConnectionPool

object H2Transactor {
  def apply[M[_]: Async](url: String, user: String, pass: String): M[H2Transactor[M]] =
    Async[M].delay(Transactor.fromDataSource[M](JdbcConnectionPool.create(url, user, pass)))
}
