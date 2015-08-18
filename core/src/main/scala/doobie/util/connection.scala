package doobie.util

import doobie.util.capture.Capture
import doobie.util.connector.Connector

import scalaz.{ Monad, Catchable }

import java.sql.Connection

/** Typeclass instances for `java.sql.Connection`. */
object connection {

  /** @group Typeclass Instances */
  implicit def connectionConnector[M[_]: Monad: Capture: Catchable]: Connector[M, Connection] =
    Connector.instance(Monad[M].point(_))

}
