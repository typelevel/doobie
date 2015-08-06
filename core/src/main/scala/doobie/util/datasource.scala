package doobie.util

import doobie.util.capture.Capture
import doobie.util.liftxa.LiftXA
import doobie.util.connector.Connector
import doobie.util.transactor.Transactor

import scalaz.{ Monad, Catchable, Lens }

import javax.sql.DataSource

object datasource {

  final case class DataSourceXA(xa: LiftXA, ds: DataSource)
  object DataSourceXA {

    def apply(ds: DataSource): DataSourceXA =
      apply(LiftXA.default, ds)

    implicit def instance[M[_]: Monad: Capture: Catchable]: Transactor[M, DataSourceXA] =
      Transactor.instance(Lens.lensu((a, b) => a.copy(xa = b), _.xa), xa => Capture[M].apply(xa.ds.getConnection))

  }

  implicit def dataSourceConnector[M[_]: Monad: Capture: Catchable]: Connector[M, DataSource] =
    Connector.instance(ds => Capture[M].apply(ds.getConnection))

}
