// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.hikari

import cats.effect.IO
import com.zaxxer.hikari.HikariConfig

class ConfigSpec extends munit.FunSuite {

  test("Default should be the same as unmodified HikariConfig") {

    import cats.effect.unsafe.implicits.global

    val actual = Config.makeHikariConfig[IO](Config()).unsafeRunSync()
    val expected = new HikariConfig()

    assertEquals(actual.getCatalog, expected.getCatalog)
    assertEquals(actual.getConnectionTimeout, expected.getConnectionTimeout)
    assertEquals(actual.getIdleTimeout, expected.getIdleTimeout)
    assertEquals(actual.getLeakDetectionThreshold, expected.getLeakDetectionThreshold)
    assertEquals(actual.getMaximumPoolSize, expected.getMaximumPoolSize)
    assertEquals(actual.getMaxLifetime, expected.getMaxLifetime)
    assertEquals(actual.getMinimumIdle, expected.getMinimumIdle)
    assertEquals(actual.getPassword, expected.getPassword)
    assertEquals(actual.getPoolName, expected.getPoolName)
    assertEquals(actual.getUsername, expected.getUsername)
    assertEquals(actual.getValidationTimeout, expected.getValidationTimeout)

    assertEquals(actual.isAllowPoolSuspension, expected.isAllowPoolSuspension)
    assertEquals(actual.isAutoCommit, expected.isAutoCommit)
    assertEquals(actual.getConnectionInitSql, expected.getConnectionInitSql)
    assertEquals(actual.getConnectionTestQuery, expected.getConnectionTestQuery)
    assertEquals(actual.getDataSourceClassName, expected.getDataSourceClassName)
    assertEquals(actual.getDataSourceJNDI, expected.getDataSourceJNDI)
    assertEquals(actual.getDriverClassName, expected.getDriverClassName)
    assertEquals(actual.getInitializationFailTimeout, expected.getInitializationFailTimeout)
    assertEquals(actual.isIsolateInternalQueries, expected.isIsolateInternalQueries)
    assertEquals(actual.getJdbcUrl, expected.getJdbcUrl)
    assertEquals(actual.isReadOnly, expected.isReadOnly)
    assertEquals(actual.isRegisterMbeans, expected.isRegisterMbeans)
    assertEquals(actual.getSchema, expected.getSchema)
    assertEquals(actual.getTransactionIsolation, expected.getTransactionIsolation)

    assertEquals(actual.getDataSource, expected.getDataSource)
    assertEquals(actual.getDataSourceProperties, expected.getDataSourceProperties)
    assertEquals(actual.getHealthCheckProperties, expected.getHealthCheckProperties)
    assertEquals(actual.getHealthCheckRegistry, expected.getHealthCheckRegistry)
    assertEquals(actual.getMetricRegistry, expected.getMetricRegistry)
    assertEquals(actual.getMetricsTrackerFactory, expected.getMetricsTrackerFactory)
    assertEquals(actual.getScheduledExecutor, expected.getScheduledExecutor)
    assertEquals(actual.getThreadFactory, expected.getThreadFactory)

  }
}
