package doobie.enum

import doobie.util.invariant._

import java.sql.ResultSet._

import scalaz.Equal
import scalaz.std.anyVal.intInstance

object holdability {

  sealed abstract class Holdability(val toInt: Int) extends Product with Serializable

  case object HoldCursorsOverCommit extends Holdability(HOLD_CURSORS_OVER_COMMIT)
  case object CloseCursorsAtCommit  extends Holdability(CLOSE_CURSORS_AT_COMMIT)

  object Holdability {

    def fromInt(n:Int): Option[Holdability] =
      Some(n) collect {
        case HoldCursorsOverCommit.toInt => HoldCursorsOverCommit
        case CloseCursorsAtCommit.toInt  => CloseCursorsAtCommit
      }

    def unsafeFromInt(n:Int): Holdability =
      fromInt(n).getOrElse(throw InvalidOrdinal[Holdability](n))

    implicit val EqualHoldability: Equal[Holdability] =
      Equal.equalBy(_.toInt)

  }

}