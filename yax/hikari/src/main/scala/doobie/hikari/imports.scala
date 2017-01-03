package doobie.hikari

object imports {
  type HikariTransactor[M[_]] = hikaritransactor.HikariTransactor[M]
  val  HikariTransactor       = hikaritransactor.HikariTransactor
}
