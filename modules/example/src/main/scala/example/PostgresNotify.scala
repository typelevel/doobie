// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

// relies on streaming, so no cats for now
package example

import cats.~>
import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.postgres._
import org.postgresql._
import fs2.Stream
import fs2.Stream._
import scala.concurrent.duration._

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

  /** A nonblocking timer for ConnectionIO. */
  implicit val ConnectionIOTimer: Timer[ConnectionIO] =
    Timer[IO].mapK(λ[IO ~> ConnectionIO](_.to[ConnectionIO]))

  /** A resource that listens on a channel and unlistens when we're done. */
  def channel(name: String): Resource[ConnectionIO, Unit] =
    Resource.make(PHC.pgListen(name) *> HC.commit)(_ => PHC.pgUnlisten(name) *> HC.commit)

  /**
    * Stream of PGNotifications on the specified channel, polling at the specified
    * rate. Note that this stream, when run, will commit the current transaction.
    */
  def notificationStream(
    channelName:     String,
    pollingInterval: FiniteDuration
  ): Stream[ConnectionIO, PGNotification] =
    for {
      _  <- resource(channel(channelName))
      _  <- awakeEvery[ConnectionIO](pollingInterval)
      ns <- eval(PHC.pgGetNotifications <* HC.commit)
      n  <- emits(ns)
    } yield n

  /** A transactor that knows how to connect to a PostgreSQL database. */
  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
  )

  /**
    * Construct a stream of PGNotifications that prints to the console. Transform it to a
    * runnable process using the transcactor above, and run it.
    */
  def run(args: List[String]): IO[ExitCode] =
    notificationStream("foo", 1.second)
      .map(n => show"${n.getPID} ${n.getName} ${n.getParameter}")
      .take(5)
      .evalMap(s => HC.delay(Console.println(s))).compile.drain
      .transact(xa)
      .as(ExitCode.Success)

}
