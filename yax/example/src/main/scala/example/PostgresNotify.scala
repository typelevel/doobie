#+cats
// relies on streaming, so no cats for now
#-cats
#+scalaz
package doobie.example

import doobie.imports._
import doobie.postgres.imports._

import org.postgresql._

import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.stream._, Process.{ eval, eval_, repeatEval, emitAll}

/**
 * Example exposing PostrgreSQL NOTIFY as a Process[ConnectionIO, PGNotification]. This will
 * likely be provided as a standard service in doobie-contrib-postgresql in a future version.
 * To play with this program, run it and then in another window do:
 *
 * > psql -d world -U postgres -c "notify foo, 'abc'"
 *
 * to send a notification. The program will exit after reading five notifications.
 */
object PostgresNotify {

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
  def notificationStream(channel: String, ms: Long): Process[ConnectionIO, PGNotification] =
    (for {
      _  <- eval(PHC.pgListen(channel) *> HC.commit)
      ns <- repeatEval(sleep(ms) *> PHC.pgGetNotifications <* HC.commit)
      n  <- emitAll(ns)
    } yield n).onComplete(eval_(PHC.pgUnlisten(channel) *> HC.commit))

  /** A transactor that knows how to connect to a PostgreSQL database. */
  val xa = Transactor.fromDriverManager[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")

  /**
   * Construct a stream of PGNotifications that prints to the console. Transform it to a
   * runnable process using the transcactor above, and run it.
   */
  def main(args: Array[String]): Unit =
    notificationStream("foo", 1000)
      .map(n => s"${n.getPID} ${n.getName} ${n.getParameter}")
      .take(5)
      .sink(s => HC.delay(Console.println(s)))
      .transact(xa)
      .void
      .unsafePerformSync

}
#-scalaz
