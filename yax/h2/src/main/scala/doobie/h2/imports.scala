package doobie.h2

#+scalaz
import doobie.util.capture.Capture
#-scalaz
#+cats
import fs2.util.{ Suspendable => Capture }
#-cats

object imports extends H2Types {

  type H2Transactor[M[_]] = h2transactor.H2Transactor[M]
  val  H2Transactor       = h2transactor.H2Transactor

  implicit def toH2TransactorOps[M[_]: Capture](h2: H2Transactor[M]) =
    new h2transactor.H2TransactorOps(h2)

}
