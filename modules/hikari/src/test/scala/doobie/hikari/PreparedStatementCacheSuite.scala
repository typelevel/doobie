// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.hikari

import cats.effect.{IO, Resource}
import com.zaxxer.hikari.HikariConfig
import org.h2.jdbc.JdbcPreparedStatement
import org.h2.jdbcx.JdbcDataSource
import org.typelevel.doobie.*
import org.typelevel.doobie.implicits.*
import org.typelevel.doobie.util.PreparedStatementCache

class PreparedStatementCacheSuite extends munit.CatsEffectSuite {

  private def transactor(cache: Boolean): Resource[IO, HikariTransactor[IO]] = {
    val h2 = new JdbcDataSource()
    h2.setURL("jdbc:h2:mem:")
    h2.setUser("sa")
    val config = new HikariConfig()
    config.setDataSource(if (cache) PreparedStatementCache.wrapDataSource(h2, maxSize = 16) else h2)
    // one physical connection, so that all transactions use it
    config.setMaximumPoolSize(1)
    HikariTransactor.fromHikariConfig[IO](config)
  }

  private val sql = "select cast(? as int)"

  private def query(n: Int): ConnectionIO[Int] = fr"select cast($n as int)".query[Int].unique

  /** Prepare `sql` through the pool, then close it. Return the statement of the JDBC driver. */
  private val driverStatement: ConnectionIO[JdbcPreparedStatement] =
    FC.prepareStatement(sql).flatMap { ps =>
      FC.delay {
        try ps.unwrap(classOf[JdbcPreparedStatement])
        finally ps.close()
      }
    }

  private def run(cache: Boolean) =
    transactor(cache).use { xa =>
      for {
        before <- driverStatement.transact(xa)
        a <- query(1).transact(xa)
        b <- query(2).transact(xa)
        after <- driverStatement.transact(xa)
      } yield ((a, b), before eq after)
    }

  test("statements are reused across transactions on a pooled connection") {
    run(cache = true).assertEquals(((1, 2), true))
  }

  test("statements are not reused without the cache") {
    run(cache = false).assertEquals(((1, 2), false))
  }
}
