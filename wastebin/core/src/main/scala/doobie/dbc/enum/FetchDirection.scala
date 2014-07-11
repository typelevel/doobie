package doobie
package dbc.enum

import java.sql.ResultSet._

sealed abstract class FetchDirection(val toInt: Int)
object FetchDirection {

  case object Forward extends FetchDirection(FETCH_FORWARD)
  case object Reverse extends FetchDirection(FETCH_REVERSE)
  case object Unknown extends FetchDirection(FETCH_UNKNOWN)

  def fromInt(n: Int): Option[FetchDirection] =
    Some(n) collect {
      case Forward.toInt => Forward
      case Reverse.toInt => Reverse
      case Unknown.toInt => Unknown
    }

  def unsafeFromInt(n: Int): FetchDirection =
    fromInt(n).getOrElse(throw InvalidOrdinal[FetchDirection](n))

}

