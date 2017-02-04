package doobie.postgres

import doobie.imports._
import doobie.postgres.imports._


import org.postgresql.PGNotification
import org.specs2.mutable.Specification

#+scalaz
import scalaz.Scalaz._
#-scalaz
#+cats
import cats.implicits._
#-cats
#+fs2
import fs2.util.Suspendable
import fs2.interop.cats._
#-fs2

object pgnotifyspec extends Specification {

  import FC.{commit, delay}

  val xa = DriverManagerTransactor[IOLite](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  // Listen on the given channel, notify on another connection
  def listen[A](channel: String, notify: ConnectionIO[A]): IOLite[List[PGNotification]] =
    (PHC.pgListen(channel) *> commit *>
     delay { Thread.sleep(50) } *>
#+scalaz
     Capture[ConnectionIO].apply(notify.transact(xa).unsafePerformIO) *>
#-scalaz
#+fs2
     Predef.implicitly[Suspendable[ConnectionIO]].delay(notify.transact(xa).unsafePerformIO) *>
#-fs2
     delay { Thread.sleep(50) } *>
     PHC.pgGetNotifications).transact(xa)

  "LISTEN/NOTIFY" should {

    "allow cross-connection notification" in  {
      val channel = "cha" + System.nanoTime
      val notify  = PHC.pgNotify(channel)
      val test    = listen(channel, notify).map(_.length)
      test.unsafePerformIO must_== 1
    }

    "allow cross-connection notification with parameter" in  {
      val channel  = "chb" + System.nanoTime
      val messages = List("foo", "bar", "baz", "qux")
      val notify   = messages.traverseU(PHC.pgNotify(channel, _))
      val test     = listen(channel, notify).map(_.map(_.getParameter))
      test.unsafePerformIO must_== messages
    }

    "collapse identical notifications" in  {
      val channel  = "chc" + System.nanoTime
      val messages = List("foo", "bar", "bar", "baz", "qux", "foo")
      val notify   = messages.traverseU(PHC.pgNotify(channel, _))
      val test     = listen(channel, notify).map(_.map(_.getParameter))
      test.unsafePerformIO must_== messages.distinct
    }

  }

}
