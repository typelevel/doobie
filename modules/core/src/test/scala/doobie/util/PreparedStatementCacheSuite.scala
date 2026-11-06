// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.util

import cats.effect.IO
import java.lang.reflect.{InvocationHandler, Method, Proxy}
import java.sql.{Connection, PreparedStatement, SQLException, Statement}
import org.h2.jdbc.JdbcPreparedStatement
import org.h2.jdbcx.JdbcDataSource
import org.typelevel.doobie.*
import org.typelevel.doobie.implicits.*

class PreparedStatementCacheSuite extends munit.CatsEffectSuite {

  // a new private in-memory database for each connection
  private def h2(): JdbcDataSource = {
    val ds = new JdbcDataSource()
    ds.setURL("jdbc:h2:mem:")
    ds.setUser("sa")
    ds
  }

  private def withConnection[A](wrap: Connection => Connection)(f: Connection => A): A = {
    val connection = wrap(h2().getConnection())
    try f(connection)
    finally connection.close()
  }

  private def cached[A](maxSize: Int)(f: Connection => A): A =
    withConnection(PreparedStatementCache.wrap(_, maxSize))(f)

  /** The statement of the JDBC driver, below the cache. */
  private def driverStatement(ps: PreparedStatement): JdbcPreparedStatement =
    ps.unwrap(classOf[JdbcPreparedStatement])

  /** Prepare `sql`, then close the statement. Return the statement of the JDBC driver. */
  private def prepareAndClose(c: Connection, sql: String): JdbcPreparedStatement = {
    val ps = c.prepareStatement(sql)
    try driverStatement(ps)
    finally ps.close()
  }

  private val select = "select cast(? as int)"

  private def selectInt(ps: PreparedStatement): Int = {
    val rs = ps.executeQuery()
    try {
      assert(rs.next())
      rs.getInt(1)
    } finally rs.close()
  }

  test("is disabled by default") {
    withConnection(identity) { c =>
      val first = prepareAndClose(c, select)
      val second = prepareAndClose(c, select)
      assert(first ne second)
      assert(first.isClosed())
    }
  }

  test("reuses a statement across doobie runs on one connection, with correct results") {
    val connection = IO(PreparedStatementCache.wrap(h2().getConnection(), maxSize = 8))
    connection.bracket { c =>
      val xa = Transactor.fromConnection[IO](c, logHandler = None)
      def q(n: Int) = sql"select cast($n as int)".query[Int].unique
      val before = prepareAndClose(c, select)
      for {
        a <- q(1).transact(xa)
        b <- q(2).transact(xa)
      } yield {
        assertEquals((a, b), (1, 2))
        val after = prepareAndClose(c, select)
        assert(before eq after)
        assert(!after.isClosed())
      }
    }(c => IO(c.close()))
  }

  test("does not leak parameters between uses") {
    cached(maxSize = 8) { c =>
      val ps1 = c.prepareStatement(select)
      val d1 = driverStatement(ps1)
      ps1.setInt(1, 42)
      assertEquals(selectInt(ps1), 42)
      ps1.close()

      val ps2 = c.prepareStatement(select)
      assert(driverStatement(ps2) eq d1)
      val _ = intercept[SQLException](ps2.executeQuery())
      ps2.setInt(1, 7)
      assertEquals(selectInt(ps2), 7)
      ps2.close()
    }
  }

  test("clears a batch and restores statement settings before reuse") {
    cached(maxSize = 8) { c =>
      val ps1 = c.prepareStatement(select)
      val d1 = driverStatement(ps1)
      val (fetchSize, maxRows, queryTimeout) = (ps1.getFetchSize(), ps1.getMaxRows(), ps1.getQueryTimeout())
      ps1.setFetchSize(fetchSize + 7)
      ps1.setMaxRows(maxRows + 3)
      ps1.setQueryTimeout(queryTimeout + 5)
      ps1.setInt(1, 1)
      ps1.addBatch()
      ps1.close()

      val ps2 = c.prepareStatement(select)
      assert(driverStatement(ps2) eq d1)
      assertEquals((ps2.getFetchSize(), ps2.getMaxRows(), ps2.getQueryTimeout()), (fetchSize, maxRows, queryTimeout))
      assertEquals(ps2.executeBatch().length, 0)
      ps2.close()
    }
  }

  test("closes result sets left open when the statement is closed") {
    cached(maxSize = 8) { c =>
      val ps = c.prepareStatement(select)
      ps.setInt(1, 1)
      val rs = ps.executeQuery()
      ps.close()
      assert(rs.isClosed())
    }
  }

  test("does not give out a statement that is in use") {
    cached(maxSize = 8) { c =>
      val ps1 = c.prepareStatement(select)
      val ps2 = c.prepareStatement(select)
      val (d1, d2) = (driverStatement(ps1), driverStatement(ps2))
      assert(d1 ne d2)
      ps1.close()
      ps2.close()
      // only one idle statement is kept per key
      assert(d2.isClosed())
      assert(prepareAndClose(c, select) eq d1)
    }
  }

  test("uses different statements for different prepare variants") {
    cached(maxSize = 8) { c =>
      val plain = prepareAndClose(c, select)
      val keys = c.prepareStatement(select, Statement.RETURN_GENERATED_KEYS)
      val names = c.prepareStatement(select, Array("x"))
      assert(driverStatement(keys) ne plain)
      assert(driverStatement(names) ne plain)
      assert(driverStatement(names) ne driverStatement(keys))
      keys.close()
      names.close()
      assert(prepareAndClose(c, select) eq plain)
    }
  }

  test("closes the least recently used statement when the cache is full") {
    cached(maxSize = 2) { c =>
      val a = prepareAndClose(c, "select 1")
      val b = prepareAndClose(c, "select 2")
      assert(prepareAndClose(c, "select 1") eq a) // b is now the least recently used
      val d = prepareAndClose(c, "select 3")
      assert(b.isClosed())
      assert(!a.isClosed())
      assert(!d.isClosed())
      assert(prepareAndClose(c, "select 2") ne b)
    }
  }

  test("closes the cached statements when the connection is closed") {
    val c = PreparedStatementCache.wrap(h2().getConnection(), maxSize = 8)
    val a = prepareAndClose(c, "select 1")
    val b = prepareAndClose(c, "select 2")
    c.close()
    assert(c.isClosed())
    assert(a.isClosed())
    assert(b.isClosed())
  }

  test("does not reuse statements across schema changes") {
    cached(maxSize = 8) { c =>
      val setup = c.createStatement()
      try
        List(
          "create schema a",
          "create schema b",
          "create table a.t (x int)",
          "create table b.t (x int)",
          "insert into a.t values (1)",
          "insert into b.t values (2)"
        ).foreach(setup.execute)
      finally setup.close()
      val sql = "select x from t where x > ?"
      def x(): Int = {
        val ps = c.prepareStatement(sql)
        try {
          ps.setInt(1, 0)
          selectInt(ps)
        } finally ps.close()
      }

      c.setSchema("A")
      assertEquals(x(), 1)
      val idle = prepareAndClose(c, "select 1")
      val inUse = c.prepareStatement(sql)
      val inUseDriver = driverStatement(inUse)
      c.setSchema("B")
      assert(idle.isClosed())
      inUse.close()
      assert(inUseDriver.isClosed()) // prepared for schema A, so not kept
      assertEquals(x(), 2)
      c.setSchema("A")
      assertEquals(x(), 1)
    }
  }

  test("closes a result set of execute() that was never read") {
    cached(maxSize = 8) { c =>
      val ps = c.prepareStatement(select)
      val d = driverStatement(ps)
      ps.setInt(1, 1)
      assert(ps.execute())
      // bypass the wrapper, so that the result set is not tracked
      val rs = d.getResultSet()
      ps.close()
      assert(rs.isClosed())
      assert(prepareAndClose(c, select) eq d)
    }
  }

  test("does not reuse a statement that was closed through its result set") {
    cached(maxSize = 8) { c =>
      val ps = c.prepareStatement(select)
      val d = driverStatement(ps)
      ps.setInt(1, 1)
      val rs = ps.executeQuery()
      rs.getStatement().close()
      ps.close()
      assert(d.isClosed())
      assert(prepareAndClose(c, select) ne d)
    }
  }

  test("closes a statement that fails to reset with a non-SQL exception") {
    var closed = false
    // a driver statement whose `clearParameters` throws
    val statement = proxy(classOf[PreparedStatement]) { (method, _) =>
      method.getName match {
        case "clearParameters" => throw new IllegalStateException("boom")
        case "close"           => closed = true; null
        case "isClosed"        => Boolean.box(closed)
        case _                 => null
      }
    }
    val connection = proxy(classOf[Connection]) { (method, _) =>
      method.getName match {
        case "prepareStatement" => statement
        case _                  => null
      }
    }
    val c = PreparedStatementCache.wrap(connection, maxSize = 8)
    c.prepareStatement(select).close()
    assert(closed)
  }

  private def proxy[A](iface: Class[A])(f: (Method, Array[AnyRef]) => AnyRef): A =
    iface.cast(
      Proxy.newProxyInstance(
        getClass.getClassLoader,
        Array[Class[?]](iface),
        new InvocationHandler {
          override def invoke(p: AnyRef, method: Method, args: Array[AnyRef]): AnyRef = f(method, args)
        }
      ))

  test("rejects a statement that was given back") {
    cached(maxSize = 8) { c =>
      val ps = c.prepareStatement(select)
      ps.close()
      assert(ps.isClosed())
      intercept[SQLException](ps.setInt(1, 1))
    }
  }

  test("returns the wrapping connection from a statement") {
    cached(maxSize = 8) { c =>
      val ps = c.prepareStatement(select)
      try assert(ps.getConnection() eq c)
      finally ps.close()
    }
  }

  test("wraps each connection of a data source") {
    val ds = PreparedStatementCache.wrapDataSource(h2(), maxSize = 8)
    withConnection(_ => ds.getConnection()) { c =>
      assert(prepareAndClose(c, select) eq prepareAndClose(c, select))
    }
  }

  test("requires a positive size") {
    val _ = intercept[IllegalArgumentException](PreparedStatementCache.wrapDataSource(h2(), maxSize = 0))
    withConnection(identity) { c =>
      intercept[IllegalArgumentException](PreparedStatementCache.wrap(c, maxSize = 0))
    }
  }
}
