// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import cats.effect.IO
import com.zaxxer.hikari.HikariDataSource
import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Strategy
import fs2.Stream

import java.sql.SQLTransientConnectionException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*

class StreamPrefetchSuite extends munit.FunSuite {

  import cats.effect.unsafe.implicits.global

  private var dataSource: HikariDataSource = null
  private var xa: Transactor[IO] = null
  private val count = 100

  private def createDataSource() = {

    Class.forName("org.postgresql.Driver")
    val dataSource = new HikariDataSource

    dataSource `setJdbcUrl` "jdbc:postgresql://localhost:5432/postgres"
    dataSource `setUsername` "postgres"
    dataSource `setPassword` "password"
    dataSource `setMaximumPoolSize` 1
    dataSource `setConnectionTimeout` 2000
    dataSource
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    dataSource = createDataSource()
    xa = Transactor.fromDataSource[IO](
      dataSource,
      ExecutionContext.fromExecutor(Executors.newFixedThreadPool(32))
    )
    val insert = for {
      _ <- Stream.eval(sql"CREATE TABLE if not exists stream_cancel_test (i text)".update.run.transact(xa))
      _ <- Stream.eval(sql"truncate table stream_cancel_test".update.run.transact(xa))
      _ <- Stream.eval(
        sql"INSERT INTO stream_cancel_test select 1 from generate_series(1, $count)".update.run.transact(xa))
    } yield ()

    insert.compile.drain.unsafeRunSync()
  }

  override def afterAll(): Unit = {
    dataSource.close()
    super.afterAll()
  }

  test("Connection returned before stream is drained, if chunk size is larger than result count") {
    val xa = Transactor.fromDataSource[IO](
      dataSource,
      ExecutionContext.fromExecutor(Executors.newFixedThreadPool(32))
    )

    val streamLargerBuffer = fr"select * from stream_cancel_test".query[Int].streamWithChunkSize(200).transact(xa)
      .evalMap(_ => fr"select 1".query[Int].unique.transact(xa))
      .compile.count

    assertEquals(streamLargerBuffer.unsafeRunSync(), count.toLong)
  }

  test("Connection is not returned after consuming only 1 chunk, if chunk size is smaller than result count") {
    val streamSmallerBuffer = fr"select * from stream_cancel_test".query[Int].streamWithChunkSize(10).transact(xa)
      .evalMap(_ => fr"select 1".query[Int].unique.transact(xa))
      .compile.count

    intercept[SQLTransientConnectionException](streamSmallerBuffer.unsafeRunSync())
  }

  test("Connection returned before stream is drained, if chunk size is smaller than result count") {
    val hasClosed = new AtomicBoolean(false)
    val xaCopy = xa.copy(
      strategy0 = Strategy.default.copy(
        always = Strategy.default.always.flatMap(_ => FC.delay(hasClosed.set(true)))
      )
    )

    val earlyClose = new AtomicBoolean(false)

    val streamSmallerBufferValid =
      fr"select * from stream_cancel_test".query[Int].streamWithChunkSize(10).transact(xaCopy)
        .evalMap { _ => IO { if (hasClosed.get()) earlyClose.set(true) } >> IO.sleep(10.milliseconds) }
        .compile.count

    assertEquals(streamSmallerBufferValid.unsafeRunSync(), count.toLong)
    assertEquals(earlyClose.get(), true)
  }

}
