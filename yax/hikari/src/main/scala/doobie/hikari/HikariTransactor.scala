package doobie.hikari

import com.zaxxer.hikari.HikariDataSource
import doobie.imports._

#+scalaz
import scalaz.{ Catchable, Monad }
import scalaz.syntax.monad._
#-scalaz
#+cats
import cats.Monad
import cats.implicits._
import fs2.interop.cats.reverse._
#-cats
#+fs2
import fs2.util.{ Catchable, Suspendable => Capture }
#-fs2

object hikaritransactor {

  type HikariTransactor[M[_]] = Transactor[M, HikariDataSource]

  implicit class HikariTransactorOps[M[_]: Capture](xa: HikariTransactor[M]) {
    /** A program that shuts down this `HikariTransactor`. */
    val shutdown: M[Unit] = xa.configure(_.close)
  }

  object HikariTransactor {
    import Predef.implicitly

    /** Constructs a program that yields an unconfigured `HikariTransactor`. */
    def initial[M[_]: Catchable: Capture](implicit M: Monad[M]): M[HikariTransactor[M]] =
      Capture[M].delay(Transactor.fromDataSource[M](new HikariDataSource)(M, implicitly, implicitly))

    /** Constructs a program that yields a `HikariTransactor` from an existing `HikariDatasource`. */
    def apply[M[_]: Catchable: Capture](hikariDataSource : HikariDataSource)(implicit M: Monad[M]): HikariTransactor[M] =
      Transactor.fromDataSource[M](new HikariDataSource)(M, implicitly, implicitly)

    /** Constructs a program that yields a `HikariTransactor` configured with the given info. */
    def apply[M[_]: Monad : Catchable : Capture](driverClassName: String, url: String, user: String, pass: String): M[HikariTransactor[M]] =
      for {
        _ <- Capture[M].delay(Class.forName(driverClassName))
        t <- initial[M]
        _ <- t.configure { ds =>
          ds setJdbcUrl  url
          ds setUsername user
          ds setPassword pass
        }
      } yield t

  }

}
