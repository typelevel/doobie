package doobie.contrib.hikari

import com.zaxxer.hikari.HikariDataSource
import doobie.imports._

import scalaz.{ Catchable, Monad }
import scalaz.syntax.id._
import scalaz.syntax.monad._

/** A `Transactor` backed by a `HikariDataSource`. */
final class HikariTransactor[M[_]: Monad : Catchable : Capture] private (ds: HikariDataSource) extends Transactor[M] {
  
  protected def connect = Capture[M].apply(ds.getConnection)

  /** A program that shuts down this `HikariTransactor`. */
  val shutdown: M[Unit] = Capture[M].apply(ds.shutdown)

  /** Constructs a program that configures the underlying `HikariDataSource`. */
  def configure(f: HikariDataSource => M[Unit]): M[Unit] = f(ds)

}

object HikariTransactor {
  
  /** Constructs a program that yields an unconfigured `HikariTransactor`. */
  def initial[M[_]: Monad : Catchable: Capture]: M[HikariTransactor[M]] = 
    Capture[M].apply(new HikariTransactor(new HikariDataSource()))

  /** Constructs a program that yields a `HikariTransactor` configured with the given info. */
  def apply[M[_]: Monad : Catchable : Capture](url: String, user: String, pass: String): M[HikariTransactor[M]] =
    for {
      t <- initial[M]
      _ <- t.configure(ds => Capture[M].apply {
        ds setJdbcUrl  url
        ds setUsername user
        ds setPassword pass
      })
    } yield t

}
