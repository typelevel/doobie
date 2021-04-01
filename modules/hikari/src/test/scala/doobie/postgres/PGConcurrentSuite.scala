// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import java.util.concurrent.Executors

import cats.effect.IO
import com.zaxxer.hikari.HikariDataSource
import doobie._
import doobie.implicits._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class PGConcurrentSuite extends munit.FunSuite {

  import cats.effect.unsafe.implicits.global

  def transactor() = {

    Class.forName("org.postgresql.Driver")
    val dataSource = new HikariDataSource

    dataSource setJdbcUrl "jdbc:postgresql://localhost:5432/postgres"
    dataSource setUsername "postgres"
    dataSource setPassword ""
    dataSource setMaximumPoolSize 10
    dataSource setConnectionTimeout 2000

    Transactor.fromDataSource[IO](
      dataSource,
      ExecutionContext.fromExecutor(Executors.newFixedThreadPool(32))
    )

  }

  test("Not leak connections with recursive query streams") {

    val xa = transactor()

    val poll: fs2.Stream[IO, Int] =
      fr"select 1".query[Int].stream.transact(xa) ++ fs2.Stream.exec(IO.sleep(50.millis))

    val pollingStream: IO[Unit] = fs2.Stream.emits(List.fill(4)(poll.repeat))
      .parJoinUnbounded
      .take(20)
      .compile
      .drain

    assertEquals(pollingStream.unsafeRunSync(), ())
  }


}
