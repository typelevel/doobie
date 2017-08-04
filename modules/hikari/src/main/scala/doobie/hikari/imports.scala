package doobie.hikari

import fs2.util.{ Suspendable => Capture }


object imports {
  type HikariTransactor[M[_]] = hikaritransactor.HikariTransactor[M]
  val  HikariTransactor       = hikaritransactor.HikariTransactor

  implicit def ToHikariTransactorOps[M[_]: Capture](xa: HikariTransactor[M]): hikaritransactor.HikariTransactorOps[M] =
    new hikaritransactor.HikariTransactorOps[M](xa)

}
