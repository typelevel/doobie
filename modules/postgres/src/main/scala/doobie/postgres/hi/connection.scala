// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.hi

import cats.~>
import cats.data.Kleisli
import org.postgresql.{ PGConnection, PGNotification }
import doobie._, doobie.implicits._
import doobie.postgres.free.{ Env, KleisliInterpreter }
import cats.effect.Blocker
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors

/** Module of safe `PGConnectionIO` operations lifted into `ConnectionIO`. */
object connection {

  // For now we're going to hardcode a daemon threaded blocker because there's no way to pass
  // through the one from the other interpreter.
  private val blocker = Blocker.liftExecutionContext(
    ExecutionContext.fromExecutor(
      Executors.newCachedThreadPool { (r: Runnable) =>
        val t = new Thread(r)
        t.setName("doobie.postgres.hi.connection.blocker")
        t.setDaemon(true)
        t
      }
    )
  )

  // An intepreter for lifting PGConnectionIO into ConnectionIO
  val defaultInterpreter: PFPC.PGConnectionOp ~> Kleisli[ConnectionIO, Env[ConnectionIO, PGConnection], ?] =
    KleisliInterpreter[ConnectionIO](blocker).PGConnectionInterpreter

  val pgGetBackendPID: ConnectionIO[Int] =
    pgGetConnection(PFPC.getBackendPID)

  def pgGetConnection[A](k: PGConnectionIO[A]): ConnectionIO[A] =
    FC.unwrap(classOf[PGConnection]).flatMap(c => k.foldMap(defaultInterpreter).run(Env(c, implicitly)))

  def pgGetCopyAPI[A](k: CopyManagerIO[A]): ConnectionIO[A] =
    pgGetConnection(PHPC.getCopyAPI(k))

  def pgGetFastpathAPI[A](k: FastpathIO[A]): ConnectionIO[A] =
    pgGetConnection(PHPC.getFastpathAPI(k))

  def pgGetLargeObjectAPI[A](k: LargeObjectManagerIO[A]): ConnectionIO[A] =
    pgGetConnection(PHPC.getLargeObjectAPI(k))

  val pgGetNotifications: ConnectionIO[List[PGNotification]] =
    pgGetConnection(PHPC.getNotifications)

  val pgGetPrepareThreshold: ConnectionIO[Int] =
    pgGetConnection(PHPC.getPrepareThreshold)

  def pgSetPrepareThreshold(threshold: Int): ConnectionIO[Unit] =
    pgGetConnection(PHPC.setPrepareThreshold(threshold))

  /**
   * Construct a program that notifies on the given channel. Note that the channel is NOT sanitized;
   * it cannot be passed as a parameter and is simply interpolated into the statement. DO NOT pass
   * user input here.
   */
  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  def pgNotify(channel: String): ConnectionIO[Unit] =
    execVoid("NOTIFY " + channel)

  /**
   * Construct a program that notifies on the given channel, with a payload. Note that neither the
   * channel nor the payload are sanitized; neither can be passed as parameters and are simply
   * interpolated into the statement. DO NOT pass user input here.
   */
  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
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
