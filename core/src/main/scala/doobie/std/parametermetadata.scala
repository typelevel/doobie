package doobie.std

import doobie.enum.jdbctype.JdbcType
import doobie.enum.parameternullable.ParameterNullable
import doobie.util.indexed.Indexed

import java.sql.ParameterMetaData

import scalaz.syntax.enum._
import scalaz.std.anyVal._

object parametermetadata {

  implicit val ParameterMetaDataIndexed: Indexed[ParameterMetaData] =
    new Indexed[ParameterMetaData] {

      def meta(a: ParameterMetaData) = 
        (1 |-> a.getParameterCount).map { i =>
          val j = JdbcType.unsafeFromInt(a.getParameterType(i))
          val n = ParameterNullable.unsafeFromInt(a.isNullable(i)).toNullability
          Indexed.Meta(j, n)
        }
  
    }

}