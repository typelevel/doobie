// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import java.util.concurrent.Executors

import cats.effect.{ Async, IO, LiftIO }
import com.zaxxer.hikari.HikariDataSource
import doobie._
import doobie.implicits._
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import cats.effect.unsafe.UnsafeRun


trait pgconcurrent[F[_]] extends Specification {

  implicit val A: Async[F]
  implicit val L: LiftIO[F]
  implicit val U: UnsafeRun[F]

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
      ExecutionContext.fromExecutor(Executors.newFixedThreadPool(32))
    )

  }

  "Not leak connections with recursive query streams" in {

    val xa = transactor()

    val poll: fs2.Stream[F, Int] =
      fr"select 1".query[Int].stream.transact(xa) ++ fs2.Stream.exec(A.sleep(50.millis))

    val pollingStream: F[Unit] = fs2.Stream.emits(List.fill(4)(poll.repeat))
      .parJoinUnbounded
      .take(20)
      .compile
      .drain

    U.unsafeRunSync(pollingStream) must_== (())
  }


}

class pgconcurrentIO extends pgconcurrent[IO] {

  import cats.effect.unsafe.implicits.global
  implicit val A: Async[IO] = IO.asyncForIO
  override implicit val L: LiftIO[IO] = LiftIO.ioLiftIO
  implicit val U: UnsafeRun[IO] = IO.unsafeRunForIO

}
