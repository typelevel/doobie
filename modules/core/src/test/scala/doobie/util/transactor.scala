// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.{ Async, IO }
import doobie._, doobie.implicits._
import org.specs2.mutable.Specification

class transactorspec extends Specification {

  val q = sql"select 42".query[Int].unique

  import cats.effect.unsafe.implicits.global

  def xa[A[_]: Async] = Transactor.fromDriverManager[A](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  "transactor" should {

    "support cats.effect.IO" in {
      q.transact(xa[IO]).unsafeRunSync() must_=== 42
    }

  }

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  class ConnectionTracker {
    var connections = List.empty[java.sql.Connection]

    def track[F[_]: Async](xa: Transactor[F]) = {
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

  "Connection lifecycle" >> {

    "Connection.close should be called on success" in {
      val tracker = new ConnectionTracker
      val transactor = tracker.track(xa[IO])
      sql"select 1".query[Int].unique.transact(transactor).unsafeRunSync()
      tracker.connections.map(_.isClosed) must_== List(true)
    }

    "Connection.close should be called on failure" in {
      val tracker = new ConnectionTracker
      val transactor = tracker.track(xa[IO])
      sql"abc".query[Int].unique.transact(transactor).attempt.unsafeRunSync().toOption must_== None
      tracker.connections.map(_.isClosed) must_== List(true)
    }

  }

  "Connection lifecycle (streaming)" >> {

    "Connection.close should be called on success" in {
      val tracker = new ConnectionTracker
      val transactor = tracker.track(xa[IO])
      sql"select 1".query[Int].stream.compile.toList.transact(transactor).unsafeRunSync()
      tracker.connections.map(_.isClosed) must_== List(true)
    }

    "Connection.close should be called on failure" in {
      val tracker = new ConnectionTracker
      val transactor = tracker.track(xa[IO])
      sql"abc".query[Int].stream.compile.toList.transact(transactor).attempt.unsafeRunSync().toOption must_== None
      tracker.connections.map(_.isClosed) must_== List(true)
    }

  }

}
