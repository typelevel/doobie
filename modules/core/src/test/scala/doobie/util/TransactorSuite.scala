// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.{ Async, ContextShift, IO }
import doobie._, doobie.implicits._
import scala.concurrent.ExecutionContext


class TransactorSuite extends munit.FunSuite {

  val q = sql"select 42".query[Int].unique

  implicit def contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  def xa[A[_]: Async: ContextShift] = Transactor.fromDriverManager[A](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  test("Transactor should support cats.effect.IO") {
    assertEquals(q.transact(xa[IO]).unsafeRunSync(), 42)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  class ConnectionTracker {
    var connections = List.empty[java.sql.Connection]

    def track[F[_]: Async: ContextShift](xa: Transactor[F]) = {
      def withA(t: doobie.util.transactor.Transactor[F]): Transactor.Aux[F, t.A] = {
        Transactor.connect.modify(t, f => a => {
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
    sql"select 1".query[Int].unique.transact(transactor).unsafeRunSync()
    assertEquals(tracker.connections.map(_.isClosed), List(true))
  }

  test("Connection.close should be called on failure") {
    val tracker = new ConnectionTracker
    val transactor = tracker.track(xa[IO])
    assertEquals(sql"abc".query[Int].unique.transact(transactor).attempt.unsafeRunSync().toOption, None)
    assertEquals(tracker.connections.map(_.isClosed), List(true))
  }

  test("[Streaming] Connection.close should be called on success") {
    val tracker = new ConnectionTracker
    val transactor = tracker.track(xa[IO])
    sql"select 1".query[Int].stream.compile.toList.transact(transactor).unsafeRunSync()
    assertEquals(tracker.connections.map(_.isClosed), List(true))
  }

  test("[Streaming] Connection.close should be called on failure") {
    val tracker = new ConnectionTracker
    val transactor = tracker.track(xa[IO])
    assertEquals(sql"abc".query[Int].stream.compile.toList.transact(transactor).attempt.unsafeRunSync().toOption, None)
    assertEquals(tracker.connections.map(_.isClosed), List(true))
  }

}
