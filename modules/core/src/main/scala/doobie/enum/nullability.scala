package doobie.enum

import doobie.enum.{ parameternullable => P }
import doobie.enum.{ columnnullable => C }

import cats.kernel.Eq
import cats.kernel.instances.int._

/** Generic nullability that subsumes JDBC's distinct parameter and column nullability. */
object nullability {

  /** @group Implementation */
  sealed abstract class Nullability {
    
    def toParameterNullable: P.ParameterNullable = 
      P.ParameterNullable.fromNullability(this)

    def toColumnNullable: C.ColumnNullable = 
      C.ColumnNullable.fromNullability(this)
      
  }

  sealed abstract class NullabilityKnown extends Nullability

  /** @group Values */ case object NoNulls         extends NullabilityKnown
  /** @group Values */ case object Nullable        extends NullabilityKnown
  /** @group Values */ case object NullableUnknown extends Nullability

  /** @group Implementation */
  object Nullability {

    def fromBoolean(b: Boolean): Nullability =
      if (b) Nullable else NoNulls

    def fromParameterNullable(pn: P.ParameterNullable): Nullability =
      pn match {
        case P.NoNulls         => NoNulls
        case P.Nullable        => Nullable
        case P.NullableUnknown => NullableUnknown
      }

    def fromColumnNullable(pn: C.ColumnNullable): Nullability =
      pn match {
        case C.NoNulls         => NoNulls
        case C.Nullable        => Nullable
        case C.NullableUnknown => NullableUnknown
      }

    implicit val EqNullability: Eq[Nullability] =
      Eq.fromUniversalEquals

  }

}
