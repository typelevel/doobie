// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie
package hikari

import cats.effect.kernel.{ Async, Resource, Sync }
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import scala.concurrent.ExecutionContext

object HikariTransactor {

  /** Construct a `HikariTransactor` from an existing `HikariDatasource`. */
  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  def apply[M[_]: Async](
    hikariDataSource : HikariDataSource,
    connectEC:         ExecutionContext
  ): HikariTransactor[M] =
    Transactor.fromDataSource[M](hikariDataSource, connectEC)

  private def createDataSourceResource[M[_]: Sync](factory: => HikariDataSource): Resource[M, HikariDataSource] = {
    val alloc = Sync[M].delay(factory)
    val free = (ds: HikariDataSource) => Sync[M].delay(ds.close())
    Resource.make(alloc)(free)
  }

  /** Resource yielding an unconfigured `HikariTransactor`. */
  def initial[M[_]: Async](
    connectEC: ExecutionContext
  ): Resource[M, HikariTransactor[M]] = {
    createDataSourceResource(new HikariDataSource)
      .map(Transactor.fromDataSource[M](_, connectEC))
  }

  /** Resource yielding a new `HikariTransactor` configured with the given HikariConfig. */
  def fromHikariConfig[M[_]: Async](
    hikariConfig: HikariConfig,
    connectEC: ExecutionContext
  ): Resource[M, HikariTransactor[M]] = {
    createDataSourceResource(new HikariDataSource(hikariConfig))
      .map(Transactor.fromDataSource[M](_, connectEC))
  }

  /** Resource yielding a new `HikariTransactor` configured with the given info. */
  def newHikariTransactor[M[_]: Async](
    driverClassName: String,
    url:             String,
    user:            String,
    pass:            String,
    connectEC:       ExecutionContext
  ): Resource[M, HikariTransactor[M]] =
    for {
      _ <- Resource.eval(Async[M].delay(Class.forName(driverClassName)))
      t <- initial[M](connectEC)
      _ <- Resource.eval {
            t.configure { ds =>
              Async[M].delay {
                ds setJdbcUrl  url
                ds setUsername user
                ds setPassword pass
              }
            }
          }
    } yield t

}
