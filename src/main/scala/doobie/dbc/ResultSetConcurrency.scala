package doobie.dbc

sealed abstract class ResultSetConcurrency(val toInt: Int)
object ResultSetConcurrency {

  case object ReadOnly  extends ResultSetConcurrency(1007)
  case object Updatable extends ResultSetConcurrency(1008)

  val Default = ReadOnly

  def fromInt(n:Int): Option[ResultSetConcurrency] =
    n match {
      case 1007 => Some(ReadOnly)
      case 1008 => Some(Updatable)
      case _    => None
    }

  def unsafeFromInt(n: Int): ResultSetConcurrency =
    fromInt(n).getOrElse(sys.error(s"ResultSetConcurrency: no such ordinal: $n"))

}
