// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.hikari

import java.util.Properties
import java.util.concurrent.{ScheduledExecutorService, ThreadFactory}

import cats.effect.Sync
import cats.syntax.show._
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.metrics.MetricsTrackerFactory
import doobie.`enum`.TransactionIsolation
import javax.sql.DataSource

import scala.concurrent.duration.Duration

/** Configuration case class, susceptible to PureConfig.
  * Helps with creating `com.zaxxer.hikari.HikariConfig`,
  * which in turn is used to create `doobie.hikari.HikariTransactor`.
  * See the method `HikariTransactor.fromConfig` */
final case class Config(catalog: Option[String] = None,
                        connectionTimeout: Option[Duration] = None,
                        idleTimeout: Option[Duration] = None,
                        leakDetectionThreshold: Option[Duration] = None,
                        maximumPoolSize: Option[Int] = None,
                        maxLifetime: Option[Duration] = None,
                        minimumIdle: Option[Int] = None,
                        password: Option[String] = None,
                        poolName: Option[String] = None,
                        username: Option[String] = None,
                        validationTimeout: Option[Duration] = None,
                        allowPoolSuspension: Option[Boolean] = None,
                        autoCommit: Option[Boolean] = None,
                        connectionInitSql: Option[String] = None,
                        connectionTestQuery: Option[String] = None,
                        dataSourceClassName: Option[String] = None,
                        dataSourceJNDI: Option[String] = None,
                        driverClassName: Option[String] = None,
                        initializationFailTimeout: Option[Duration] = None,
                        isolateInternalQueries: Option[Boolean] = None,
                        jdbcUrl: Option[String] = None,
                        readOnly: Option[Boolean] = None,
                        registerMbeans: Option[Boolean] = None,
                        schema: Option[String] = None,
                        transactionIsolation: Option[TransactionIsolation] =
                          None,
)

object Config {
  def makeHikariConfig[F[_]](
    config: Config,
    dataSource: Option[DataSource] = None,
    dataSourceProperties: Option[Properties] = None,
    healthCheckProperties: Option[Properties] = None,
    healthCheckRegistry: Option[Object] = None,
    metricRegistry: Option[Object] = None,
    metricsTrackerFactory: Option[MetricsTrackerFactory] = None,
    scheduledExecutor: Option[ScheduledExecutorService] = None,
    threadFactory: Option[ThreadFactory] = None,
  )(implicit F: Sync[F]): F[HikariConfig] =
    F.delay {
      val c = new HikariConfig()

      config.catalog.foreach(c.setCatalog)
      config.connectionTimeout.map(_.toMillis).foreach(c.setConnectionTimeout)
      config.idleTimeout.map(_.toMillis).foreach(c.setIdleTimeout)
      config.leakDetectionThreshold
        .map(_.toMillis)
        .foreach(c.setLeakDetectionThreshold)
      config.maximumPoolSize.foreach(c.setMaximumPoolSize)
      config.maxLifetime.map(_.toMillis).foreach(c.setMaxLifetime)
      config.minimumIdle.foreach(c.setMinimumIdle)
      config.password.foreach(c.setPassword)
      config.poolName.foreach(c.setPoolName)
      config.username.foreach(c.setUsername)
      config.validationTimeout.map(_.toMillis).foreach(c.setValidationTimeout)

      config.allowPoolSuspension.foreach(c.setAllowPoolSuspension)
      config.autoCommit.foreach(c.setAutoCommit)
      config.connectionInitSql.foreach(c.setConnectionInitSql)
      config.connectionTestQuery.foreach(c.setConnectionTestQuery)
      config.dataSourceClassName.foreach(c.setDataSourceClassName)
      config.dataSourceJNDI.foreach(c.setDataSourceJNDI)
      config.driverClassName.foreach(c.setDriverClassName)
      config.initializationFailTimeout
        .map(_.toMillis)
        .foreach(c.setInitializationFailTimeout)
      config.isolateInternalQueries.foreach(c.setIsolateInternalQueries)
      config.jdbcUrl.foreach(c.setJdbcUrl)
      config.readOnly.foreach(c.setReadOnly)
      config.registerMbeans.foreach(c.setRegisterMbeans)
      config.schema.foreach(c.setSchema)
      config.transactionIsolation.map(_.show).foreach(c.setTransactionIsolation)

      dataSource.foreach(c.setDataSource)
      dataSourceProperties.foreach(c.setDataSourceProperties)
      healthCheckProperties.foreach(c.setHealthCheckProperties)
      healthCheckRegistry.foreach(c.setHealthCheckRegistry)
      metricRegistry.foreach(c.setMetricRegistry)
      metricsTrackerFactory.foreach(c.setMetricsTrackerFactory)
      scheduledExecutor.foreach(c.setScheduledExecutor)
      threadFactory.foreach(c.setThreadFactory)

      c
    }
}
