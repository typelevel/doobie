package doobie.h2

import doobie.util.capture.Capture

object imports extends H2Types {

  type H2Transactor[M[_]] = h2transactor.H2Transactor[M]
  val  H2Transactor       = h2transactor.H2Transactor

  implicit def toH2TransactorOps[M[_]: Capture](h2: H2Transactor[M]) =
    new h2transactor.H2TransactorOps(h2)

}
