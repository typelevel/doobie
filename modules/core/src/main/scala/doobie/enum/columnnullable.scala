// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.enum

import doobie.util.invariant._

import java.sql.ResultSetMetaData._

import cats.kernel.Eq
import cats.kernel.instances.int._

/** @group Implementation */
sealed abstract class ColumnNullable(val toInt: Int) {
  def toNullability: Nullability =
    Nullability.fromColumnNullable(this)
}

/** @group Implementation */
object ColumnNullable {

  /** @group Values */ case object NoNulls         extends ColumnNullable(columnNoNulls)
  /** @group Values */ case object Nullable        extends ColumnNullable(columnNullable)
  /** @group Values */ case object NullableUnknown extends ColumnNullable(columnNullableUnknown)

  def fromInt(n:Int): Option[ColumnNullable] =
    Some(n) collect {
      case NoNulls.toInt         => NoNulls
      case Nullable.toInt        => Nullable
      case NullableUnknown.toInt => NullableUnknown
    }

  def fromNullability(n: Nullability): ColumnNullable =
    n match {
      case Nullability.NoNulls         => NoNulls
      case Nullability.Nullable        => Nullable
      case Nullability.NullableUnknown => NullableUnknown
    }

  def unsafeFromInt(n: Int): ColumnNullable =
    fromInt(n).getOrElse(throw InvalidOrdinal[ColumnNullable](n))

  implicit val EqColumnNullable: Eq[ColumnNullable] =
    Eq.by(_.toInt)

}
