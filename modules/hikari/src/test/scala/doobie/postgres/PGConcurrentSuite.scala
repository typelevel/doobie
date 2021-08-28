// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import java.util.concurrent.Executors

import cats.effect.syntax.effect._
import cats.effect.{ConcurrentEffect, IO}
import com.zaxxer.hikari.HikariDataSource
import doobie._
import doobie.implicits._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import cats.effect.Temporal

class PGConcurrentSuiteIO extends PGConcurrentSuite[IO] {
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val E: ConcurrentEffect[IO] = IO.ioConcurrentEffect
  implicit def T: Temporal[IO] = IO.timer(scala.concurrent.ExecutionContext.global)
}


trait PGConcurrentSuite[F[_]] extends munit.FunSuite {

  implicit def E: ConcurrentEffect[F]
  implicit def T: Temporal[F]
  implicit def contextShift: ContextShift[F]

  def transactor() = {

    Class.forName("org.postgresql.Driver")
    val dataSource = new HikariDataSource

    dataSource setJdbcUrl "jdbc:postgresql://localhost:5432/postgres"
    dataSource setUsername "postgres"
    dataSource setPassword ""
    dataSource setMaximumPoolSize 10
    dataSource setConnectionTimeout 2000

    Transactor.fromDataSource[F](
      dataSource,
      ExecutionContext.fromExecutor(Executors.newFixedThreadPool(32)),
      Blocker.liftExecutorService(Executors.newCachedThreadPool())
    )

  }

  test("Not leak connections with recursive query streams") {

    val xa = transactor()

    val poll: fs2.Stream[F, Int] =
      fr"select 1".query[Int].stream.transact(xa) ++ fs2.Stream.eval_(T.sleep(50.millis))

    val pollingStream: F[Unit] = fs2.Stream.emits(List.fill(4)(poll.repeat))
      .parJoinUnbounded
      .take(20)
      .compile
      .drain

    assertEquals(pollingStream.toIO.unsafeRunSync(), ())
  }


}
