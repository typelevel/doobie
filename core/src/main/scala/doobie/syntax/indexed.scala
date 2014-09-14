package doobie.syntax

import doobie.enum.jdbctype.JdbcType
import doobie.enum.nullability.Nullability
import doobie.util.indexed.Indexed

object indexed {

  implicit class IndexedOps[A](a: A)(implicit A: Indexed[A]) {

    def meta: List[Indexed.Meta] =
      A.meta(a)

  }

}