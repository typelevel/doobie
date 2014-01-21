package doobie.dbc

sealed abstract class IsolationLevel(val toInt: Int)
object IsolationLevel {

  case object LevelNone            extends IsolationLevel(0)
  case object LevelReadUncommitted extends IsolationLevel(1)
  case object LevelReadCommitted   extends IsolationLevel(2)
  case object LevelRepeatableRead  extends IsolationLevel(4)
  case object LevelSerializable    extends IsolationLevel(8)

  def fromInt(n: Int): Option[IsolationLevel] =
    n match {
      case 0 => Some(LevelNone)
      case 1 => Some(LevelReadUncommitted)
      case 2 => Some(LevelReadCommitted)
      case 4 => Some(LevelRepeatableRead)
      case 8 => Some(LevelSerializable)
      case _ => None
    }

  def unsafeFromInt(n: Int): IsolationLevel =
    fromInt(n).getOrElse(sys.error(s"IsolationLevel: no such ordinal: $n"))

}