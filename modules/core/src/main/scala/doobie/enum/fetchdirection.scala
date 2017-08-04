package doobie.enum

import doobie.util.invariant._

import java.sql.ResultSet._

import cats.kernel.Eq
import cats.kernel.instances.int._

object fetchdirection {

  /** @group Implementation */
  sealed abstract class FetchDirection(val toInt: Int)

  /** @group Values */ case object Forward extends FetchDirection(FETCH_FORWARD)
  /** @group Values */ case object Reverse extends FetchDirection(FETCH_REVERSE)
  /** @group Values */ case object Unknown extends FetchDirection(FETCH_UNKNOWN)

  /** @group Implementation */
  object FetchDirection {

    def fromInt(n: Int): Option[FetchDirection] =
      Some(n) collect {
        case Forward.toInt => Forward
        case Reverse.toInt => Reverse
        case Unknown.toInt => Unknown
      }

    def unsafeFromInt(n: Int): FetchDirection =
      fromInt(n).getOrElse(throw InvalidOrdinal[FetchDirection](n))

    implicit val EqFetchDirection: Eq[FetchDirection] =
      Eq.by(_.toInt)

  }

}
