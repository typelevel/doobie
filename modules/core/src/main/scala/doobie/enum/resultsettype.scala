// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.enum

import doobie.util.invariant._

import java.sql.ResultSet._

import cats.kernel.Eq
import cats.kernel.instances.int._

/** @group Implementation */
sealed abstract class ResultSetType(val toInt: Int)

/** @group Implementation */
object ResultSetType {

  /** @group Values */ case object TypeForwardOnly       extends ResultSetType(TYPE_FORWARD_ONLY)
  /** @group Values */ case object TypeScrollInsensitive extends ResultSetType(TYPE_SCROLL_INSENSITIVE)
  /** @group Values */ case object TypeScrollSensitive   extends ResultSetType(TYPE_SCROLL_SENSITIVE)

  def fromInt(n: Int): Option[ResultSetType] =
    Some(n) collect {
      case TypeForwardOnly.toInt       => TypeForwardOnly
      case TypeScrollInsensitive.toInt => TypeScrollInsensitive
      case TypeScrollSensitive.toInt   => TypeScrollSensitive
    }

  def unsafeFromInt(n: Int): ResultSetType =
    fromInt(n).getOrElse(throw InvalidOrdinal[ResultSetType](n))

  implicit val EqResultSetType: Eq[ResultSetType] =
    Eq.by(_.toInt)
}
