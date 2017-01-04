package doobie.h2

object imports extends H2Types {

  type H2Transactor[M[_]] = h2transactor.H2Transactor[M]
  val  H2Transactor       = h2transactor.H2Transactor

}
