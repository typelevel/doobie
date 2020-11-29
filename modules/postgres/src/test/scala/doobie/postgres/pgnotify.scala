// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import cats.effect.IO
import cats.syntax.all._
import doobie._, doobie.implicits._
import org.postgresql.PGNotification
import org.specs2.mutable.Specification
import scala.concurrent.duration._

class pgnotifyspec extends Specification {

  import cats.effect.unsafe.implicits.global

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  // Listen on the given channel, notify on another connection
  def listen[A](channel: String, notify: ConnectionIO[A]): IO[List[PGNotification]] =
    xa.liftF(lift =>
      PHC.pgListen(channel) *> 
      FC.commit *>
      lift(IO.sleep(50.milli)) *>
      lift(notify.transact(xa)) *>
      lift(IO.sleep(50.milli)) *>
      PHC.pgGetNotifications
    )

  "LISTEN/NOTIFY" should {

    "allow cross-connection notification" in  {
      val channel = "cha" + System.nanoTime.toString
      val notify  = PHC.pgNotify(channel)
      val test    = listen(channel, notify).map(_.length)
      test.unsafeRunSync() must_== 1
    }

    "allow cross-connection notification with parameter" in  {
      val channel  = "chb" + System.nanoTime.toString
      val messages = List("foo", "bar", "baz", "qux")
      val notify   = messages.traverse(PHC.pgNotify(channel, _))
      val test     = listen(channel, notify).map(_.map(_.getParameter))
      test.unsafeRunSync() must_== messages
    }

    "collapse identical notifications" in  {
      val channel  = "chc" + System.nanoTime.toString
      val messages = List("foo", "bar", "bar", "baz", "qux", "foo")
      val notify   = messages.traverse(PHC.pgNotify(channel, _))
      val test     = listen(channel, notify).map(_.map(_.getParameter))
      test.unsafeRunSync() must_== messages.distinct
    }

  }

}
