package doobie.util

import doobie.enum.jdbctype.JdbcType
import doobie.enum.nullability.Nullability

/** Typeclass for heterogeneous indexed JDBC types. */
object indexed {
  
  trait Indexed[A] {
    def meta(a: A): List[Indexed.Meta]
  }

  object Indexed {
    final case class Meta(jdbcType: JdbcType, nullability: Nullability)
  }

}

