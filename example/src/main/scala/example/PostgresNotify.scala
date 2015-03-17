package doobie.example

import doobie.imports._

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

  /** Program that retrieves the underlying PGConnection */
  val getPGConnection: ConnectionIO[PGConnection] =
    FC.unwrap(classOf[PGConnection])

  /** Program that gets all new notifications. */
  val getNotifications: ConnectionIO[List[PGNotification]] =
    getPGConnection.flatMap(c => HC.delay(c.getNotifications).map {
      case null => Nil
      case as   => as.toList  
    })

  /** Construct a program that execs a no-param statement and discards the return value */
  def execVoid(sql: String): ConnectionIO[Unit] =
    HC.prepareStatement(sql)(HPS.executeUpdate).void

  /** Construct a program that starts listening on the given channel. */
  def listen(channel: String): ConnectionIO[Unit] = 
    execVoid("LISTEN " + channel)

  /** Construct a program that stops listening on the given channel. */
  def unlisten(channel: String): ConnectionIO[Unit] = 
    execVoid("UNLISTEN " + channel)

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
      _  <- eval(listen(channel) *> HC.commit)
      ns <- repeatEval(sleep(ms) *> getNotifications <* HC.commit)
      n  <- emitAll(ns)
    } yield n).onComplete(eval_(unlisten(channel) *> HC.commit))

  /** A transactor that knows how to connect to a PostgreSQL database. */
  val xa: Transactor[Task] = 
    DriverManagerTransactor("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")

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
      .run

}
