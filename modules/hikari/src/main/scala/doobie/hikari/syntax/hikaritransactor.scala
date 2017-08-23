package doobie
package hikari
package syntax

import cats.effect.Sync

final class HikariTransactorOps[M[_]: Sync](xa: HikariTransactor[M]) {
  /** A program that shuts down this `HikariTransactor`. */
  val shutdown: M[Unit] = xa.configure(_.close)
}

trait ToHikariTransactorOps {
  implicit def toHikariTransactorOps[M[_]: Sync](xa: HikariTransactor[M]): HikariTransactorOps[M] =
    new HikariTransactorOps(xa)
}

object hikaritransactor extends ToHikariTransactorOps
