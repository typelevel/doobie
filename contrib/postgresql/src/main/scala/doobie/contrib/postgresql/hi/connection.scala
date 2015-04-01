package doobie.contrib.postgresql.hi

import org.postgresql.{ PGConnection, PGNotification }

import doobie.contrib.postgresql.free.{ pgconnection => FPGC }
import doobie.contrib.postgresql.hi.{ pgconnection => HPGC }
import doobie.contrib.postgresql.free.pgconnection.PGConnectionIOOps
import doobie.imports._

import scalaz.syntax.functor._

/** Module of safe `PGConnectionIO` operations lifted into `ConnectionIO`. */
object connection {

  def liftPGConnection[A](k: FPGC.PGConnectionIO[A]): ConnectionIO[A] =
    FC.unwrap(classOf[PGConnection]) >>= k.transK[ConnectionIO]

  val pgGetNotifications: ConnectionIO[List[PGNotification]] =
    liftPGConnection(HPGC.getNotifications)

  val pgGetPrepareThreshold: ConnectionIO[Int] =
    liftPGConnection(HPGC.getPrepareThreshold)

  def pgSetPrepareThreshold(threshold: Int): ConnectionIO[Unit] =
    liftPGConnection(HPGC.setPrepareThreshold(threshold))

  private def execVoid(sql: String): ConnectionIO[Unit] =
    HC.prepareStatement(sql)(HPS.executeUpdate).void

  /** Construct a program that starts listening on the given channel. */
  def pgListen(channel: String): ConnectionIO[Unit] = 
    execVoid("LISTEN " + channel)

  /** Construct a program that stops listening on the given channel. */
  def pgUnlisten(channel: String): ConnectionIO[Unit] = 
    execVoid("UNLISTEN " + channel)

}
