// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import cats.effect.{IO}
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import org.postgresql.PGNotification

class NotifySuite extends munit.CatsEffectSuite {
  import FC.{commit, delay}
  import PostgresTestTransactor.xa

  // Listen on the given channel, notify on another connection
  def listen[A](channel: String, notify: ConnectionIO[A]): IO[List[PGNotification]] =
    WeakAsync.liftIO[ConnectionIO].use { liftIO =>
      (for {
        _ <- PHC.pgListen(channel)
        _ <- commit
        _ <- delay { Thread.sleep(50) }
        _ <- liftIO.liftIO(notify.transact(xa))
        _ <- commit
        _ <- delay { Thread.sleep(50) }
        notifications <- PHC.pgGetNotifications
      } yield notifications).transact(xa)
    }

  test("LISTEN/NOTIFY should allow cross-connection notification") {
    val channel = "cha" + System.nanoTime.toString
    val notify = PHC.pgNotify(channel)
    val test = listen(channel, notify).map(_.length)
    test.assertEquals(1)
  }

  test("LISTEN/NOTIFY should allow cross-connection notification with parameter") {
    val channel = "chb" + System.nanoTime.toString
    val messages = List("foo", "bar", "baz", "qux")
    val notify = messages.traverse(PHC.pgNotify(channel, _))
    val test = listen(channel, notify).map(_.map(_.getParameter))
    test.assertEquals(messages)
  }

  test("LISTEN/NOTIFY should collapse identical notifications") {
    val channel = "chc" + System.nanoTime.toString
    val messages = List("foo", "bar", "bar", "baz", "qux", "foo")
    val notify = messages.traverse(PHC.pgNotify(channel, _))
    val test = listen(channel, notify).map(_.map(_.getParameter))
    test.assertEquals(messages.distinct)
  }
}
