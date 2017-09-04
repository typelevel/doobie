// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.enum

import doobie.util.invariant._

import java.sql.Connection._

import cats.kernel.Eq
import cats.kernel.instances.int._

/** @group Implementation */
sealed abstract class TransactionIsolation(val toInt: Int)

/** @group Implementation */
object TransactionIsolation {

  /** @group Values */ case object TransactionNone            extends TransactionIsolation(TRANSACTION_NONE)
  /** @group Values */ case object TransactionReadUncommitted extends TransactionIsolation(TRANSACTION_READ_UNCOMMITTED)
  /** @group Values */ case object TransactionReadCommitted   extends TransactionIsolation(TRANSACTION_READ_COMMITTED)
  /** @group Values */ case object TransactionRepeatableRead  extends TransactionIsolation(TRANSACTION_REPEATABLE_READ)
  /** @group Values */ case object TransactionSerializable    extends TransactionIsolation(TRANSACTION_SERIALIZABLE)

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

  implicit val EqTransactionIsolation: Eq[TransactionIsolation] =
    Eq.by(_.toInt)

}
