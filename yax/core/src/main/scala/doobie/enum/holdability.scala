package doobie.enum

import doobie.util.invariant._

import java.sql.ResultSet._

#+scalaz
import scalaz.Equal
import scalaz.std.anyVal.intInstance
#-scalaz
#+cats
import cats.kernel.Eq
import cats.kernel.std.int._
#-cats

object holdability {

  /** @group Implementation */
  sealed abstract class Holdability(val toInt: Int) extends Product with Serializable
  
  /** @group Values */ case object HoldCursorsOverCommit extends Holdability(HOLD_CURSORS_OVER_COMMIT)
  /** @group Values */ case object CloseCursorsAtCommit  extends Holdability(CLOSE_CURSORS_AT_COMMIT)

  /** @group Implementation */
  object Holdability {

    def fromInt(n:Int): Option[Holdability] =
      Some(n) collect {
        case HoldCursorsOverCommit.toInt => HoldCursorsOverCommit
        case CloseCursorsAtCommit.toInt  => CloseCursorsAtCommit
      }

    def unsafeFromInt(n:Int): Holdability =
      fromInt(n).getOrElse(throw InvalidOrdinal[Holdability](n))

#+scalaz
    implicit val EqualHoldability: Equal[Holdability] =
      Equal.equalBy(_.toInt)
#-scalaz
#+cats
    implicit val EqHoldability: Eq[Holdability] =
      Eq.by(_.toInt)
#-cats

  }

}