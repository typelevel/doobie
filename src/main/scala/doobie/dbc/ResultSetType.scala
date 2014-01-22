package doobie.dbc

import java.sql.ResultSet._

sealed abstract class ResultSetType(val toInt: Int)
object ResultSetType {

  case object TypeForwardOnly       extends ResultSetType(TYPE_FORWARD_ONLY)
  case object TypeScrollInsensitive extends ResultSetType(TYPE_SCROLL_INSENSITIVE)
  case object TypeScrollSensitive   extends ResultSetType(TYPE_SCROLL_SENSITIVE)

  def fromInt(n: Int): Option[ResultSetType] =
    Some(n) collect {
      case TypeForwardOnly.toInt       => TypeForwardOnly
      case TypeScrollInsensitive.toInt => TypeScrollInsensitive
      case TypeScrollSensitive.toInt   => TypeScrollSensitive
    }

  def unsafeFromInt(n: Int): ResultSetType =
    fromInt(n).getOrElse(sys.error(s"ResultSetType: no such ordinal: $n"))

}