package doobie.enum

import doobie.util.invariant._
import doobie.enum.{ nullability => N }

import java.sql.ResultSetMetaData._

#+scalaz
import scalaz.Equal
import scalaz.std.anyVal.intInstance
#-scalaz
#+cats
import cats.kernel.Eq
import cats.kernel.instances.int._
#-cats

object columnnullable {

  /** @group Implementation */
  sealed abstract class ColumnNullable(val toInt: Int) {
    def toNullability: N.Nullability =
      N.Nullability.fromColumnNullable(this) 
  }

  /** @group Values */ case object NoNulls         extends ColumnNullable(columnNoNulls)
  /** @group Values */ case object Nullable        extends ColumnNullable(columnNullable)
  /** @group Values */ case object NullableUnknown extends ColumnNullable(columnNullableUnknown)

  /** @group Implementation */
  object ColumnNullable {

    def fromInt(n:Int): Option[ColumnNullable] =
      Some(n) collect {
        case NoNulls.toInt         => NoNulls
        case Nullable.toInt        => Nullable
        case NullableUnknown.toInt => NullableUnknown
      }

    def fromNullability(n: N.Nullability): ColumnNullable =
      n match {
        case N.NoNulls         => NoNulls
        case N.Nullable        => Nullable
        case N.NullableUnknown => NullableUnknown
      }

    def unsafeFromInt(n: Int): ColumnNullable =
      fromInt(n).getOrElse(throw InvalidOrdinal[ColumnNullable](n))

#+scalaz
    implicit val EqualColumnNullable: Equal[ColumnNullable] =
      Equal.equalBy(_.toInt)
#-scalaz
#+cats
    implicit val EqColumnNullable: Eq[ColumnNullable] =
      Eq.by(_.toInt)
#-cats

  }

}