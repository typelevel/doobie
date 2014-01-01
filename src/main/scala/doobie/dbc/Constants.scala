package doobie
package dbc

////// TRANSACTION ISOLATION

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

////// HOLDABILITY

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

////// RESULTSET TYPE

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

////// RESULTSET CONCURRENCY

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

////// FETCH DIRECTION

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

