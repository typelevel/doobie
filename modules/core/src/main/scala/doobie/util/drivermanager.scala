package doobie.util

import doobie.free.drivermanager.{ DriverManagerIO, delay, getConnection }
import doobie.util.capture.Capture
import doobie.util.liftxa.LiftXA
import doobie.util.transactor.Transactor

import scalaz.{ Monad, Catchable, Lens }
import scalaz.syntax.apply._

import java.sql.Connection

object drivermanager {

  final case class DriverManagerXA(xa: LiftXA, dmio: DriverManagerIO[Connection])

  object DriverManagerXA {

    private def create(driver: String, connect: DriverManagerIO[Connection]): DriverManagerXA =
      DriverManagerXA(LiftXA.default, delay(Class.forName(driver)) *> connect)

    def apply(driver: String, url: String): DriverManagerXA =
      create(driver, getConnection(url))

    def apply(driver: String, url: String, user: String, pass: String): DriverManagerXA =
      create(driver, getConnection(url, user, pass))

    def apply(driver: String, url: String, info: java.util.Properties): DriverManagerXA =
      create(driver, getConnection(url, info))

    implicit def instance[M[_]: Monad: Capture: Catchable]: Transactor[M, DriverManagerXA] =
      Transactor.instance(Lens.lensu((a, b) => a.copy(xa = b), _.xa), _.dmio.trans[M])

  }

}
