// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie
package hikari

import cats.effect.{ Async, ContextShift }
import cats.implicits._
import com.zaxxer.hikari.HikariDataSource
import fs2.Stream

object HikariTransactor {

  /** Constructs a program that yields an unconfigured `HikariTransactor`. */
  def initial[M[_]: Async: ContextShift]: M[HikariTransactor[M]] =
    Async[M].delay(Transactor.fromDataSource[M](new HikariDataSource))

  /** Constructs a program that yields a `HikariTransactor` from an existing `HikariDatasource`. */
  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  def apply[M[_]: Async: ContextShift](hikariDataSource : HikariDataSource): HikariTransactor[M] =
    Transactor.fromDataSource[M](hikariDataSource)

  /** Constructs a program that yields a new `HikariTransactor` configured with the given info. */
  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  @deprecated("This method has been renamed `newHikariTransactor` to help clarify usage", "doobie 0.5.0")
  def apply[M[_]: Async: ContextShift](driverClassName: String, url: String, user: String, pass: String): M[HikariTransactor[M]] =
    newHikariTransactor(driverClassName, url, user, pass)

  /** Constructs a program that yields a new `HikariTransactor` configured with the given info. */
  def newHikariTransactor[M[_]: Async: ContextShift](driverClassName: String, url: String, user: String, pass: String): M[HikariTransactor[M]] =
    for {
      _ <- Async[M].delay(Class.forName(driverClassName))
      t <- initial[M]
      _ <- t.configure { ds =>
        Async[M].delay {
          ds setJdbcUrl  url
          ds setUsername user
          ds setPassword pass
        }
      }
    } yield t

  /** Constructs a stream that emits a single `HikariTransactor` with guaranteed cleanup. */
  def stream[M[_]: Async: ContextShift](driverClassName: String, url: String, user: String, pass: String) : Stream[M, HikariTransactor[M]] =
    Stream.bracket(newHikariTransactor(driverClassName, url, user, pass))(_.configure(ds => Async[M].delay(ds.close)))

}
