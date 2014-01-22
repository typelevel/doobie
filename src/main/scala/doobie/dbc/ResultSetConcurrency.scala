package doobie.dbc

import java.sql.ResultSet._

sealed abstract class ResultSetConcurrency(val toInt: Int)
object ResultSetConcurrency {

  case object ConcurReadOnly  extends ResultSetConcurrency(CONCUR_READ_ONLY)
  case object ConcurUpdatable extends ResultSetConcurrency(CONCUR_UPDATABLE)

  def fromInt(n:Int): Option[ResultSetConcurrency] =
    Some(n) collect {
      case ConcurReadOnly.toInt  => ConcurReadOnly
      case ConcurUpdatable.toInt => ConcurUpdatable
    }

  def unsafeFromInt(n: Int): ResultSetConcurrency =
    fromInt(n).getOrElse(sys.error(s"ResultSetConcurrency: no such ordinal: $n"))

}
