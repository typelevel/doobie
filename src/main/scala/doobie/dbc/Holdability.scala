package doobie.dbc
import java.sql.ResultSet._

sealed abstract class Holdability(val toInt: Int)
object Holdability {

  case object HoldCursorsOverCommit extends Holdability(HOLD_CURSORS_OVER_COMMIT)
  case object CloseCursorsAtCommit  extends Holdability(CLOSE_CURSORS_AT_COMMIT)

  def fromInt(n:Int): Option[Holdability] =
    Some(n) collect {
      case HoldCursorsOverCommit.toInt => HoldCursorsOverCommit
      case CloseCursorsAtCommit.toInt  => CloseCursorsAtCommit
    }

  def unsafeFromInt(n:Int): Holdability =
    fromInt(n).getOrElse(sys.error(s"Holdability: no such ordinal: $n"))

}

