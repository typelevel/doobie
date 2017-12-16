// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie
package hikari
package syntax

import cats.effect.Sync

final class HikariTransactorOps[M[_]: Sync](xa: HikariTransactor[M]) {
  /** A program that shuts down this `HikariTransactor`. */
  val shutdown: M[Unit] = xa.configure(ds => Sync[M].delay(ds.close))
}

trait ToHikariTransactorOps {
  implicit def toHikariTransactorOps[M[_]: Sync](xa: HikariTransactor[M]): HikariTransactorOps[M] =
    new HikariTransactorOps(xa)
}

object hikaritransactor extends ToHikariTransactorOps
