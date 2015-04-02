package doobie.contrib.postgresql.hi

import org.postgresql.{ PGConnection, PGNotification }

import doobie.contrib.postgresql.free.pgconnection.PGConnectionIO
import doobie.contrib.postgresql.free.copymanager.CopyManagerIO
import doobie.contrib.postgresql.free.fastpath.FastpathIO
import doobie.contrib.postgresql.free.largeobjectmanager.LargeObjectManagerIO

import doobie.contrib.postgresql.hi.{ pgconnection => HPGC }

import doobie.imports._

import scalaz.syntax.functor._

/** Module of safe `PGConnectionIO` operations lifted into `ConnectionIO`. */
object connection {

  val pgGetBackendPID: ConnectionIO[Int] =
    pgGetConnection(HPGC.getBackendPID)

  def pgGetConnection[A](k: PGConnectionIO[A]): ConnectionIO[A] =
    FC.unwrap(classOf[PGConnection]) >>= k.transK[ConnectionIO]

  def pgGetCopyAPI[A](k: CopyManagerIO[A]): ConnectionIO[A] =
    pgGetConnection(HPGC.getCopyAPI(k))

  def pgGetFastpathAPI[A](k: FastpathIO[A]): ConnectionIO[A] =
    pgGetConnection(HPGC.getFastpathAPI(k))

  def pgGetLargeObjectAPI[A](k: LargeObjectManagerIO[A]): ConnectionIO[A] =
    pgGetConnection(HPGC.getLargeObjectAPI(k))

  val pgGetNotifications: ConnectionIO[List[PGNotification]] =
    pgGetConnection(HPGC.getNotifications)

  val pgGetPrepareThreshold: ConnectionIO[Int] =
    pgGetConnection(HPGC.getPrepareThreshold)

  def pgSetPrepareThreshold(threshold: Int): ConnectionIO[Unit] =
    pgGetConnection(HPGC.setPrepareThreshold(threshold))

  /** Construct a program that starts listening on the given channel. */
  def pgListen(channel: String): ConnectionIO[Unit] = 
    execVoid("LISTEN " + channel)

  /** Construct a program that stops listening on the given channel. */
  def pgUnlisten(channel: String): ConnectionIO[Unit] = 
    execVoid("UNLISTEN " + channel)


  // a helper
  private def execVoid(sql: String): ConnectionIO[Unit] =
    HC.prepareStatement(sql)(HPS.executeUpdate).void

}
