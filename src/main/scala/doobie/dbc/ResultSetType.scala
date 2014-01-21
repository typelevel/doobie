package doobie.dbc

sealed abstract class ResultSetType(val toInt: Int)
object ResultSetType {

  case object ForwardOnly       extends ResultSetType(1003)
  case object ScrollInsensitive extends ResultSetType(1004)
  case object ScrollSensitive   extends ResultSetType(1005)

  val Default = ForwardOnly

  def fromInt(n: Int): Option[ResultSetType] =
    n match {
      case 1003 => Some(ForwardOnly)
      case 1004 => Some(ScrollInsensitive)
      case 1005 => Some(ScrollSensitive)
      case _    => None
    }

  def unsafeFromInt(n: Int): ResultSetType =
    fromInt(n).getOrElse(sys.error(s"ResultSetType: no such ordinal: $n"))

}