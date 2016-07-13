package doobie.contrib.postgresql.hi

import org.postgresql.{ PGConnection, PGNotification }

import doobie.contrib.postgresql.free.pgconnection.PGConnectionIO
import doobie.contrib.postgresql.free.copymanager.CopyManagerIO
import doobie.contrib.postgresql.free.fastpath.FastpathIO
import doobie.contrib.postgresql.free.largeobjectmanager.LargeObjectManagerIO

import doobie.contrib.postgresql.hi.{ pgconnection => HPGC }

import doobie.imports._

/** Module of safe `PGConnectionIO` operations lifted into `ConnectionIO`. */
object connection {

  val pgGetBackendPID: ConnectionIO[Int] =
    pgGetConnection(HPGC.getBackendPID)

  def pgGetConnection[A](k: PGConnectionIO[A]): ConnectionIO[A] =
    FC.unwrap(classOf[PGConnection]).flatMap(k.transK[ConnectionIO].run)

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

  /** 
   * Construct a program that notifies on the given channel. Note that the channel is NOT sanitized;
   * it cannot be passed as a parameter and is simply interpolated into the statement. DO NOT pass 
   * user input here.
   */
  def pgNotify(channel: String): ConnectionIO[Unit] = 
    execVoid("NOTIFY " + channel)

  /** 
   * Construct a program that notifies on the given channel, with a payload. Note that neither the 
   * channel nor the payload are sanitized; neither can be passed as parameters and are simply 
   * interpolated into the statement. DO NOT pass user input here.
   */
  def pgNotify(channel: String, payload: String): ConnectionIO[Unit] = 
    execVoid(s"NOTIFY $channel, '$payload'")

  /** 
   * Construct a program that starts listening on the given channel. Note that the channel is NOT 
   * sanitized; it cannot be passed as a parameter and is simply interpolated into the statement. 
   * DO NOT pass user input here.
   */
  def pgListen(channel: String): ConnectionIO[Unit] = 
    execVoid("LISTEN " + channel)

  /** 
   * Construct a program that stops listening on the given channel. Note that the channel is NOT 
   * sanitized; it cannot be passed as a parameter and is simply interpolated into the statement. 
   * DO NOT pass user input here.
   */
  def pgUnlisten(channel: String): ConnectionIO[Unit] = 
    execVoid("UNLISTEN " + channel)


  // a helper
  private def execVoid(sql: String): ConnectionIO[Unit] =
    HC.prepareStatement(sql)(HPS.executeUpdate).map(_ => ())

}
