package doobie.hikari

import cats.effect.{ Sync, Async }
import cats.implicits._
import com.zaxxer.hikari.HikariDataSource
import doobie.imports._

object hikaritransactor {

  type HikariTransactor[M[_]] = Transactor.Aux[M, HikariDataSource]

  implicit class HikariTransactorOps[M[_]: Sync](xa: HikariTransactor[M]) {
    /** A program that shuts down this `HikariTransactor`. */
    val shutdown: M[Unit] = xa.configure(_.close)
  }

  object HikariTransactor {

    /** Constructs a program that yields an unconfigured `HikariTransactor`. */
    def initial[M[_]: Async]: M[HikariTransactor[M]] =
      Async[M].delay(Transactor.fromDataSource[M](new HikariDataSource))

    /** Constructs a program that yields a `HikariTransactor` from an existing `HikariDatasource`. */
    def apply[M[_]: Async](hikariDataSource : HikariDataSource): HikariTransactor[M] =
      Transactor.fromDataSource[M](hikariDataSource)

    /** Constructs a program that yields a `HikariTransactor` configured with the given info. */
    def apply[M[_]: Async](driverClassName: String, url: String, user: String, pass: String): M[HikariTransactor[M]] =
      for {
        _ <- Async[M].delay(Class.forName(driverClassName))
        t <- initial[M]
        _ <- t.configure { ds =>
          ds setJdbcUrl  url
          ds setUsername user
          ds setPassword pass
        }
      } yield t

  }

}
