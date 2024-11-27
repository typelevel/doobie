// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.{Async, IO}
import cats.implicits.catsSyntaxApplicativeId
import doobie.*
import doobie.implicits.*

class TransactorSuite extends munit.CatsEffectSuite {

  import cats.effect.unsafe.implicits.global

  val q = sql"select 42".query[Int].unique

  def xa[A[_]: Async] = Transactor.fromDriverManager[A](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None
  )

  test("Transactor should support cats.effect.IO") {
    q.transact(xa[IO]) assertEquals (42)
  }

  class ConnectionTracker {
    var connections = List.empty[java.sql.Connection]

    def track[F[_]](xa: Transactor[F]) = {
      def withA(t: doobie.util.transactor.Transactor[F]): Transactor.Aux[F, t.A] = {
        Transactor.connect.modify(
          t,
          f =>
            a => {
              f(a).map { conn =>
                connections = conn :: connections
                conn
              }
            })
      }
      withA(xa)
    }
  }

  test("Connection.close should be called on success") {
    val tracker = new ConnectionTracker
    val transactor = tracker.track(xa[IO])
    val _ = sql"select 1".query[Int].unique.transact(transactor).map(_ =>
      tracker.connections.map(_.isClosed)).assertEquals(List(true))
  }

  test("Connection.close should be called on failure") {
    val tracker = new ConnectionTracker
    val transactor = tracker.track(xa[IO])
    sql"abc".query[Int].unique.transact(transactor).attempt.map(_.isLeft).assertEquals(true)
  }

  test("[Streaming] Connection.close should be called on success") {
    val tracker = new ConnectionTracker
    val transactor = tracker.track(xa[IO])
    sql"select 1".query[Int].stream.compile.toList.transact(transactor).map(_ =>
      tracker.connections.map(_.isClosed)).assertEquals(List(true))
  }

  test("[Streaming] Connection.close should be called on failure") {
    val tracker = new ConnectionTracker
    val transactor = tracker.track(xa[IO])
    sql"abc".query[Int].stream.compile.toList.transact(transactor).attempt.map(_.isLeft).assertEquals(true)
  }

}
