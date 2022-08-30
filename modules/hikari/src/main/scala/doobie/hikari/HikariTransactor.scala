// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie
package hikari

import java.util.Properties
import java.util.concurrent.{ScheduledExecutorService, ThreadFactory}

import cats.effect.implicits._
import cats.effect.kernel.{ Async, Resource, Sync }
import com.zaxxer.hikari.metrics.MetricsTrackerFactory
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import javax.sql.DataSource

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

  /** Resource yielding a new `HikariTransactor` configured with the given Config.
   * Unless you have a good reason, consider using `fromConfigAutoEc` which creates the `connectEC` for you.
   */
  def fromConfig[M[_]: Async](
    config: Config,
    connectEC: ExecutionContext,
    dataSource: Option[DataSource] = None,
    dataSourceProperties: Option[Properties] = None,
    healthCheckProperties: Option[Properties] = None,
    healthCheckRegistry: Option[Object] = None,
    metricRegistry: Option[Object] = None,
    metricsTrackerFactory: Option[MetricsTrackerFactory] = None,
    scheduledExecutor: Option[ScheduledExecutorService] = None,
    threadFactory: Option[ThreadFactory] = None,
  ): Resource[M, HikariTransactor[M]] = {
    Resource
      .liftK(
        Config.makeHikariConfig(
          config,
          dataSource,
          dataSourceProperties,
          healthCheckProperties,
          healthCheckRegistry,
          metricRegistry,
          metricsTrackerFactory,
          scheduledExecutor,
          threadFactory
        )
      )
      .flatMap(fromHikariConfig(_, connectEC))
  }

  /** Resource yielding a new `HikariTransactor` configured with the given Config.
   * The `connectEC` is created automatically, with the same size as the Hikari pool.
   */
  def fromConfigAutoEc[M[_]: Async](
      config: Config,
      dataSource: Option[DataSource] = None,
      dataSourceProperties: Option[Properties] = None,
      healthCheckProperties: Option[Properties] = None,
      healthCheckRegistry: Option[Object] = None,
      metricRegistry: Option[Object] = None,
      metricsTrackerFactory: Option[MetricsTrackerFactory] = None,
      scheduledExecutor: Option[ScheduledExecutorService] = None,
      threadFactory: Option[ThreadFactory] = None,
    ): Resource[M, HikariTransactor[M]] = {
    Resource
      .liftK(
        Config.makeHikariConfig(
          config,
          dataSource,
          dataSourceProperties,
          healthCheckProperties,
          healthCheckRegistry,
          metricRegistry,
          metricsTrackerFactory,
          scheduledExecutor,
          threadFactory
        )
      )
      .flatMap(fromHikariConfig(_))
  }

  /** Resource yielding a new `HikariTransactor` configured with the given HikariConfig.
   * Unless you have a good reason, consider using the overload without explicit `connectEC`, it will be created automatically for you.
   */
  def fromHikariConfig[M[_]: Async](
    hikariConfig: HikariConfig,
    connectEC: ExecutionContext
  ): Resource[M, HikariTransactor[M]] = {
    createDataSourceResource(new HikariDataSource(hikariConfig))
      .map(Transactor.fromDataSource[M](_, connectEC))
  }

  /** Resource yielding a new `HikariTransactor` configured with the given HikariConfig.
   * The `connectEC` is created automatically, with the same size as the Hikari pool.
   */
  def fromHikariConfig[M[_]: Async](hikariConfig: HikariConfig): Resource[M, HikariTransactor[M]] =
  for {
    _ <- Sync[M].delay(hikariConfig.validate()).toResource // to populate unset fields with default values, like `maximumPoolSize`
    // Also note that the number of JDBC connections is usually limited by the underlying JDBC pool. You may therefore want to limit your connection pool to the same size as the underlying JDBC pool as any additional threads are guaranteed to be blocked.
    // https://tpolecat.github.io/doobie/docs/14-Managing-Connections.html#about-threading
    connectEC <- ExecutionContexts.fixedThreadPool(hikariConfig.getMaximumPoolSize)
    result <- createDataSourceResource(new HikariDataSource(hikariConfig))
      .map(Transactor.fromDataSource[M](_, connectEC))
  } yield result

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
