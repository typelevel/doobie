// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.enum

import doobie.util.invariant._

import java.sql.ParameterMetaData._

import cats.kernel.Eq
import cats.kernel.instances.int._

/** @group Implementation */
sealed abstract class ParameterNullable(val toInt: Int) {
  def toNullability: Nullability =
    Nullability.fromParameterNullable(this)
}

/** @group Implementation */
object ParameterNullable {

  /** @group Values */ case object NoNulls         extends ParameterNullable(parameterNoNulls)
  /** @group Values */ case object Nullable        extends ParameterNullable(parameterNullable)
  /** @group Values */ case object NullableUnknown extends ParameterNullable(parameterNullableUnknown)

  def fromInt(n:Int): Option[ParameterNullable] =
    Some(n) collect {
      case NoNulls.toInt         => NoNulls
      case Nullable.toInt        => Nullable
      case NullableUnknown.toInt => NullableUnknown
    }

  def fromNullability(n: Nullability): ParameterNullable =
    n match {
      case Nullability.NoNulls         => NoNulls
      case Nullability.Nullable        => Nullable
      case Nullability.NullableUnknown => NullableUnknown
    }

  def unsafeFromInt(n: Int): ParameterNullable =
    fromInt(n).getOrElse(throw InvalidOrdinal[ParameterNullable](n))

  implicit val EqParameterNullable: Eq[ParameterNullable] =
    Eq.by(_.toInt)

}
