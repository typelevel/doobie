// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.h2
package syntax

import cats.effect.Sync

final class H2TransactorOps[M[_]](h2: H2Transactor[M])(implicit ev: Sync[M]) {

  /** A program that shuts down this `H2Transactor`. */
  val dispose: M[Unit] = h2.configure(pool => ev.delay(pool.dispose))

  /** Returns the number of active (open) connections of the underlying `JdbcConnectionPool`. */
  val getActiveConnections: M[Int] = h2.configure(pool => ev.delay(pool.getActiveConnections))

  /** Gets the maximum time in seconds to wait for a free connection. */
  val getLoginTimeout: M[Int] = h2.configure(pool => ev.delay(pool.getLoginTimeout))

  /** Gets the maximum number of connections to use. */
  val getMaxConnections: M[Int] = h2.configure(pool => ev.delay(pool.getMaxConnections))

  /** Sets the maximum time in seconds to wait for a free connection. */
  def setLoginTimeout(seconds: Int): M[Unit] = h2.configure(pool => ev.delay(pool.setLoginTimeout(seconds)))

  /** Sets the maximum number of connections to use from now on. */
  def setMaxConnections(max: Int): M[Unit] = h2.configure(pool => ev.delay(pool.setMaxConnections(max)))

}

trait ToH2TransactorOps {
  implicit def toH2TransactorOps[M[_]: Sync](h2: H2Transactor[M]): H2TransactorOps[M] =
    new H2TransactorOps(h2)
}

object h2transactor extends ToH2TransactorOps
