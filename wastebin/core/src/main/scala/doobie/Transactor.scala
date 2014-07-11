package doobie

import dbc.Connection
import dbc.connection.{ setAutoCommit, rollback, close }
import scala.reflect.Manifest
import scalaz.effect.IO
import scalaz.syntax.effect.monadCatchIO._
import scalaz.syntax.monad._
import java.sql

trait Logger {
  def exec[A](f: Log => IO[A]): IO[A]
}

/** A logger that dumps the log to the console if the operation fails. */
object ConsoleLogger extends Logger {
  def exec[A](f: Log => IO[A]): IO[A] =
    for {
      e <- IO(LogElement(System.currentTimeMillis.toString))
      l <- util.TreeLogger.newLogger(e)
      a <- f(l) onException l.dump
    } yield a
}

trait Transactor {
  def exec[A](action: Connection[A]): IO[A] 
}

class DriverManagerTransactor(
  driverClass: String, 
  url: String, user: String, pass: String, 
  logger: Logger = ConsoleLogger) extends Transactor {

  def exec[A](action: Connection[A]): IO[A] =
    logger.exec { l => 
      for {
        _ <- l.log(LogElement("load driver"), IO(Class.forName(driverClass)))
        c <- l.log(LogElement(s"getConnection($url, $user, ***)"), IO(sql.DriverManager.getConnection(url, user, pass)))
        _ <- setAutoCommit(false).run((l,c)) // hmmm
        a <- l.log(LogElement("try/finally"), (action ensuring (rollback >> close)).run((l, c)))
      } yield a
    }

}

object DriverManagerTransactor {

  def apply[A](url: String, user: String, pass: String)(implicit A: Manifest[A]): DriverManagerTransactor =
    new DriverManagerTransactor(A.runtimeClass.getName, url, user, pass)

}
