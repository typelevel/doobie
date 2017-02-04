package doobie.h2

import doobie.imports._

import org.h2.jdbcx.JdbcConnectionPool

#+scalaz
import scalaz.{ Catchable, Monad }
#-scalaz
#+cats
import cats.Monad
#-cats
#+fs2
import fs2.util.{ Catchable, Suspendable => Capture }
#-fs2

/** Module for a `Transactor` backed by an H2 `JdbcConnectionPool`. */
object h2transactor {

  type H2Transactor[M[_]] = Transactor[M, JdbcConnectionPool]

  implicit class H2TransactorOps[M[_]: Capture](h2: H2Transactor[M]) {

    /** A program that shuts down this `H2Transactor`. */
    val dispose: M[Unit] = h2.configure(_.dispose)

    /** Returns the number of active (open) connections of the underlying `JdbcConnectionPool`. */
    val getActiveConnections: M[Int] = h2.configure(_.getActiveConnections)

    /** Gets the maximum time in seconds to wait for a free connection. */
    val getLoginTimeout: M[Int] = h2.configure(_.getLoginTimeout)

    /** Gets the maximum number of connections to use. */
    val getMaxConnections: M[Int] = h2.configure(_.getMaxConnections)

    /** Sets the maximum time in seconds to wait for a free connection. */
    def setLoginTimeout(seconds: Int): M[Unit] = h2.configure(_.setLoginTimeout(seconds))

    /** Sets the maximum number of connections to use from now on. */
    def setMaxConnections(max: Int): M[Unit] = h2.configure(_.setMaxConnections(max))

  }

  object H2Transactor {
    def apply[M[_]: Monad : Catchable : Capture](url: String, user: String, pass: String): M[H2Transactor[M]] =
      Capture[M].delay(Transactor.fromDataSource[M](JdbcConnectionPool.create(url, user, pass)))
  }

}
