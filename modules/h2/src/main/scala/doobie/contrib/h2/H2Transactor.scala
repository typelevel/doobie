package doobie.contrib.h2

import doobie.imports._

import org.h2.jdbcx.JdbcConnectionPool

import scalaz.{ Catchable, Monad }
import scalaz.Lens

/** Module for a `Transactor` backed by an H2 `JdbcConnectionPool`. */
object h2transactor {

  final class H2XA private (private val xa: LiftXA, private val ds: JdbcConnectionPool) {

    /** A program that shuts down this `H2Transactor`. */
    def dispose[M[_]: Capture]: M[Unit] = Capture[M].apply(ds.dispose)

    /** Returns the number of active (open) connections of the underlying `JdbcConnectionPool`. */
    def getActiveConnections[M[_]: Capture]: M[Int] = Capture[M].apply(ds.getActiveConnections)

    /** Gets the maximum time in seconds to wait for a free connection. */
    def getLoginTimeout[M[_]: Capture]: M[Int] = Capture[M].apply(ds.getLoginTimeout)

    /** Gets the maximum number of connections to use. */
    def getMaxConnections[M[_]: Capture]: M[Int] = Capture[M].apply(ds.getMaxConnections)

    /** Sets the maximum time in seconds to wait for a free connection. */
    def setLoginTimeout[M[_]: Capture](seconds: Int): M[Unit] = Capture[M].apply(ds.setLoginTimeout(seconds))

    /** Sets the maximum number of connections to use from now on. */
    def setMaxConnections[M[_]: Capture](max: Int): M[Unit] = Capture[M].apply(ds.setMaxConnections(max))

  }
  
  object H2XA {
  
    /** Constructs a program that yields a `H2Transactor` configured with the given info. */
    def apply[M[_]: Capture](url: String, user: String, pass: String): M[H2XA] =
      Capture[M].apply(new H2XA(LiftXA.default, JdbcConnectionPool.create(url, user, pass)))

    /* H2XA is a Transactor for any effect-capturing M. */
    implicit def jdbcConnectionPool[M[_]: Monad: Catchable](implicit c: Capture[M]): Transactor[M, H2XA] =
      Transactor.instance[M, H2XA](Lens.lensu((a, b) => new H2XA(b, a.ds), _.xa), h2 => c(h2.ds.getConnection))

  }

  /* JdbcConnectionPool is a Connector for any effect-capturing M. */
  implicit def jdbcConnectionPool[M[_]: Monad: Catchable: Capture]: Connector[M, JdbcConnectionPool] =
    Connector.instance(ds => Capture[M].apply(ds.getConnection))

  @deprecated("Use H2XA instead.", "0.3.0") type H2Transactor = H2XA
  @deprecated("Use H2XA instead.", "0.3.0") val  H2Transactor = H2XA

} 