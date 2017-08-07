package doobie.hikari

import cats.effect.Sync

object imports {
  type HikariTransactor[M[_]] = hikaritransactor.HikariTransactor[M]
  val  HikariTransactor       = hikaritransactor.HikariTransactor

  implicit def ToHikariTransactorOps[M[_]: Sync](xa: HikariTransactor[M]): hikaritransactor.HikariTransactorOps[M] =
    new hikaritransactor.HikariTransactorOps[M](xa)

}
