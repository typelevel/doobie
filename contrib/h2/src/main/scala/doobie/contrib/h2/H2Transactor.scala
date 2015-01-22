package doobie.contrib.h2

import doobie.imports._

import org.h2.jdbcx.JdbcConnectionPool

import scalaz.{ Catchable, Monad }
import scalaz.syntax.id._
import scalaz.syntax.monad._

/** A `Transactor` backed by an H2 `JdbcConnectionPool`. */
final class H2Transactor[M[_]: Monad : Catchable : Capture] private (ds: JdbcConnectionPool) extends Transactor[M] {
  
  protected val connect = Capture[M].apply(ds.getConnection)

  /** A program that shuts down this `H2Transactor`. */
  val dispose: M[Unit] = Capture[M].apply(ds.dispose)

  /** Returns the number of active (open) connections of the underlying `JdbcConnectionPool`. */
  val getActiveConnections: M[Int] = Capture[M].apply(ds.getActiveConnections)

  /** Gets the maximum time in seconds to wait for a free connection. */
  val getLoginTimeout: M[Int] = Capture[M].apply(ds.getLoginTimeout)

  /** Gets the maximum number of connections to use. */
  val getMaxConnections: M[Int] = Capture[M].apply(ds.getMaxConnections)

  /** Sets the maximum time in seconds to wait for a free connection. */
  def setLoginTimeout(seconds: Int): M[Unit] = Capture[M].apply(ds.setLoginTimeout(seconds))

  /** Sets the maximum number of connections to use from now on. */
  def setMaxConnections(max: Int): M[Unit] = Capture[M].apply(ds.setMaxConnections(max))

}

object H2Transactor {
  
  /** Constructs a program that yields a `H2Transactor` configured with the given info. */
  def apply[M[_]: Monad : Catchable : Capture](url: String, user: String, pass: String): M[H2Transactor[M]] =
    Capture[M].apply(new H2Transactor(JdbcConnectionPool.create(url, user, pass)))

}
