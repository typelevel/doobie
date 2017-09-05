// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.enum

import doobie.util.invariant._

import java.sql.ResultSet._

import cats.kernel.Eq
import cats.kernel.instances.int._

/** @group Implementation */
sealed abstract class ResultSetConcurrency(val toInt: Int)

/** @group Implementation */
object ResultSetConcurrency {

  /** @group Values */ case object ConcurReadOnly  extends ResultSetConcurrency(CONCUR_READ_ONLY)
  /** @group Values */ case object ConcurUpdatable extends ResultSetConcurrency(CONCUR_UPDATABLE)

  def fromInt(n:Int): Option[ResultSetConcurrency] =
    Some(n) collect {
      case ConcurReadOnly.toInt  => ConcurReadOnly
      case ConcurUpdatable.toInt => ConcurUpdatable
    }

  def unsafeFromInt(n: Int): ResultSetConcurrency =
    fromInt(n).getOrElse(throw InvalidOrdinal[ResultSetConcurrency](n))

  implicit val EqResultSetConcurrency: Eq[ResultSetConcurrency] =
    Eq.by(_.toInt)


}
