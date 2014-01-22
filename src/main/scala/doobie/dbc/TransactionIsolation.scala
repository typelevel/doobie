package doobie.dbc

import java.sql.Connection._

sealed abstract class TransactionIsolation(val toInt: Int)
object TransactionIsolation {

  case object TransactionNone            extends TransactionIsolation(TRANSACTION_NONE)
  case object TransactionReadUncommitted extends TransactionIsolation(TRANSACTION_READ_UNCOMMITTED)
  case object TransactionReadCommitted   extends TransactionIsolation(TRANSACTION_READ_COMMITTED)
  case object TransactionRepeatableRead  extends TransactionIsolation(TRANSACTION_REPEATABLE_READ)
  case object TransactionSerializable    extends TransactionIsolation(TRANSACTION_SERIALIZABLE)

  def fromInt(n: Int): Option[TransactionIsolation] =
    Some(n) collect {
      case TransactionNone.toInt            => TransactionNone
      case TransactionReadUncommitted.toInt => TransactionReadUncommitted
      case TransactionReadCommitted.toInt   => TransactionReadCommitted
      case TransactionRepeatableRead.toInt  => TransactionRepeatableRead
      case TransactionSerializable.toInt    => TransactionSerializable
    }

  def unsafeFromInt(n: Int): TransactionIsolation =
    fromInt(n).getOrElse(sys.error(s"TransactionIsolation: no such ordinal: $n"))

}