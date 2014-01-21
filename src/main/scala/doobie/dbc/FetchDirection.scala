package doobie.dbc

sealed abstract class FetchDirection(val toInt: Int)
object FetchDirection {

  case object Forward extends FetchDirection(1000)
  case object Reverse extends FetchDirection(1001)
  case object Unknown extends FetchDirection(1002)

  def fromInt(n: Int): Option[FetchDirection] =
    n match {
      case 1000 => Some(Forward)
      case 1001 => Some(Reverse)
      case 1002 => Some(Unknown)
      case _    => None
    }

  def unsafeFromInt(n: Int): FetchDirection =
    fromInt(n).getOrElse(sys.error(s"FetchDirection: no such ordinal: $n"))

}

