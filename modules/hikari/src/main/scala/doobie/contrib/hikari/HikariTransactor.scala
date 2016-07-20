package doobie.contrib.hikari

import com.zaxxer.hikari.HikariDataSource
import doobie.imports._

import scalaz.{ Catchable, Monad, Lens }
import scalaz.syntax.monad._

object hikaritransactor {

  final class HikariXA private (private val xa: LiftXA, private val ds: HikariDataSource) {
    
    /** A program that shuts down this `HikariXA`. */
    def shutdown[M[_]: Capture]: M[Unit] = Capture[M].apply(ds.shutdown)

    /** Constructs a program that configures the underlying `HikariDataSource`. */
    def configure[M[_]: Capture](f: HikariDataSource => M[Unit]): M[Unit] = f(ds)

  }

  object HikariXA {
    
    /** Constructs a `HikariTransactor` from an existing `HikariDatasource`. */
    def apply(hikariDataSource : HikariDataSource): HikariXA = 
      new HikariXA(LiftXA.default, hikariDataSource)

    /** Constructs a program that yields an unconfigured `HikariXA`. */
    def initial[M[_]: Monad : Catchable: Capture]: M[HikariXA] = 
      Capture[M].apply(new HikariXA(LiftXA.default, new HikariDataSource))

    /** Constructs a program that yields a `HikariXA` configured with the given info. */
    def apply[M[_]: Monad : Catchable : Capture](driverClassName: String, url: String, user: String, pass: String): M[HikariXA] =
      for {
        _ <- Capture[M].apply(Class.forName(driverClassName))
        t <- initial[M]
        _ <- t.configure(ds => Capture[M].apply {
          ds setJdbcUrl  url
          ds setUsername user
          ds setPassword pass
        })
      } yield t

    /* HikariXA is a Transactor for any effect-capturing M. */
    implicit def jdbcConnectionPool[M[_]: Monad: Catchable](implicit c: Capture[M]): Transactor[M, HikariXA] =
      Transactor.instance[M, HikariXA](Lens.lensu((a, b) => new HikariXA(b, a.ds), _.xa), h2 => c(h2.ds.getConnection))

  }

  /* JdbcConnectionPool is a Connector for any effect-capturing M. */
  implicit def hikariDataSourceConnector[M[_]: Monad: Catchable: Capture]: Connector[M, HikariDataSource] =
    Connector.instance(ds => Capture[M].apply(ds.getConnection))

  @deprecated("Use HikariXA instead.", "0.3.0") type HikariTransactor = HikariXA
  @deprecated("Use HikariXA instead.", "0.3.0") val  HikariTransactor = HikariXA

}

