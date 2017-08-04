package doobie.hikari

import doobie.util.capture.Capture


object imports {
  type HikariTransactor[M[_]] = hikaritransactor.HikariTransactor[M]
  val  HikariTransactor       = hikaritransactor.HikariTransactor

  implicit def ToHikariTransactorOps[M[_]: Capture](xa: HikariTransactor[M]): hikaritransactor.HikariTransactorOps[M] =
    new hikaritransactor.HikariTransactorOps[M](xa)

}
