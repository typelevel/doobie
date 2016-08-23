package doobie.enum

import doobie.util.invariant._

import java.sql.Connection._

#+scalaz
import scalaz.Equal
import scalaz.std.anyVal.intInstance
#-scalaz
#+cats
import cats.kernel.Eq
import cats.kernel.instances.int._
#-cats

object transactionisolation {

  /** @group Implementation */
  sealed abstract class TransactionIsolation(val toInt: Int)
  
  /** @group Values */ case object TransactionNone            extends TransactionIsolation(TRANSACTION_NONE)
  /** @group Values */ case object TransactionReadUncommitted extends TransactionIsolation(TRANSACTION_READ_UNCOMMITTED)
  /** @group Values */ case object TransactionReadCommitted   extends TransactionIsolation(TRANSACTION_READ_COMMITTED)
  /** @group Values */ case object TransactionRepeatableRead  extends TransactionIsolation(TRANSACTION_REPEATABLE_READ)
  /** @group Values */ case object TransactionSerializable    extends TransactionIsolation(TRANSACTION_SERIALIZABLE)

  /** @group Implementation */
  object TransactionIsolation {

    def fromInt(n: Int): Option[TransactionIsolation] =
      Some(n) collect {
        case TransactionNone.toInt            => TransactionNone
        case TransactionReadUncommitted.toInt => TransactionReadUncommitted
        case TransactionReadCommitted.toInt   => TransactionReadCommitted
        case TransactionRepeatableRead.toInt  => TransactionRepeatableRead
        case TransactionSerializable.toInt    => TransactionSerializable
      }

    def unsafeFromInt(n: Int): TransactionIsolation =
      fromInt(n).getOrElse(throw InvalidOrdinal[TransactionIsolation](n))

#+scalaz
    implicit val EqualTransactionIsolation: Equal[TransactionIsolation] =
      Equal.equalBy(_.toInt)
#-scalaz
#+cats
    implicit val EqTransactionIsolation: Eq[TransactionIsolation] =
      Eq.by(_.toInt)
#-cats

  }

}