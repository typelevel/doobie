// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

// relies on streaming, so no cats for now
package example

import cats.effect.{ IO, IOApp, ExitCode }
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.postgres._
import org.postgresql._
import fs2.Stream
import fs2.Stream._
import java.util.concurrent._
import scala.concurrent._

/**
  * Example exposing PostrgreSQL NOTIFY as a Process[ConnectionIO, PGNotification]. This will
  * likely be provided as a standard service in doobie-postgres in a future version.
  * To play with this program, run it and then in another window do:
  *
  * > psql -d world -U postgres -c "notify foo, 'abc'"
  *
  * to send a notification. The program will exit after reading five notifications.
  */
object PostgresNotify extends IOApp {

  // Use a dedicated thread to check for notifications. Otherwise, end up blocking the
  // thread of execution.
  private val pgPool = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))

  /**
    * Construct a program that pauses the current thread. This doesn't scale, but neither do
    * long- running connection-bound operations like NOTIFY/LISTEN. So the approach here is to
    * burn a thread reading the events and multplex somewhere downstream.
    */
  def sleep(ms: Long): ConnectionIO[Unit] =
    HC.delay(Thread.sleep(ms))

  /**
    * Construct a stream of PGNotifications on the specified channel, polling at the specified
    * rate. Note that this process, when run, will commit the current transaction.
    */
  def notificationStream(channel: String, ms: Long): Stream[ConnectionIO, PGNotification] =
    (for {
      _  <- eval(PHC.pgListen(channel) *> HC.commit)
      // repeatedly check for notifications, but use a different thread pool so we don't
      // block execution of the "main" thread.
      ns <- repeatEval(ContextShiftConnectionIO.evalOn(pgPool)(sleep(ms) *> PHC.pgGetNotifications <* HC.commit))
      n  <- emits(ns)
    } yield n).onComplete(eval_(PHC.pgUnlisten(channel) *> HC.commit))

  /** A transactor that knows how to connect to a PostgreSQL database. */
  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
  )

  /**
    * Construct a stream of PGNotifications that prints to the console. Transform it to a
    * runnable process using the transcactor above, and run it.
    */
  def run(args: List[String]): IO[ExitCode] =
    notificationStream("foo", 1000)
      .map(n => s"${n.getPID} ${n.getName} ${n.getParameter}")
      .take(5)
      .evalMap(s => HC.delay(Console.println(s))).compile.drain
      .transact(xa)
      .as(ExitCode.Success)

}
