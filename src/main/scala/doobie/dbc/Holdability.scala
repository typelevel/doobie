package doobie.dbc

sealed abstract class Holdability(val toInt: Int)
object Holdability {

  case object HoldCursorsOverCommit extends Holdability(1)
  case object CloseCursorsAtCommit  extends Holdability(2)

  def fromInt(n:Int): Option[Holdability] =
    n match {
      case 1 => Some(HoldCursorsOverCommit)
      case 2 => Some(CloseCursorsAtCommit)
      case _ => None
    }

  def unsafeFromInt(n:Int): Holdability =
    fromInt(n).getOrElse(sys.error(s"Holdability: no such ordinal: $n"))

}