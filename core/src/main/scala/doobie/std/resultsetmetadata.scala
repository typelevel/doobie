package doobie.std

import doobie.enum.jdbctype.JdbcType
import doobie.enum.columnnullable.ColumnNullable
import doobie.util.indexed.Indexed

import java.sql.ResultSetMetaData

import scalaz.syntax.enum._
import scalaz.std.anyVal._

object resultsetmetadata {

  implicit val ResultSetMetaDataIndexed: Indexed[ResultSetMetaData] =
    new Indexed[ResultSetMetaData] {

      def meta(a: ResultSetMetaData) = 
        (1 |-> a.getColumnCount).map { i => 
          val j = JdbcType.unsafeFromInt(a.getColumnType(i))
          val n = ColumnNullable.unsafeFromInt(a.isNullable(i)).toNullability
          Indexed.Meta(j, n)
        }
      
    }

}

