package doobie

import java.sql.Types
import scalaz._
import Scalaz._

case class JdbcType(toInt: Int, name: String)

object JdbcType {

  val ARRAY = JdbcType(Types.ARRAY, "ARRAY")
  val BIGINT = JdbcType(Types.BIGINT, "BIGINT")
  val BINARY = JdbcType(Types.BINARY, "BINARY")
  val BIT = JdbcType(Types.BIT, "BIT")
  val BLOB = JdbcType(Types.BLOB, "BLOB")
  val BOOLEAN = JdbcType(Types.BOOLEAN, "BOOLEAN")
  val CHAR = JdbcType(Types.CHAR, "CHAR")
  val CLOB = JdbcType(Types.CLOB, "CLOB")
  val DATALINK = JdbcType(Types.DATALINK, "DATALINK")
  val DATE = JdbcType(Types.DATE, "DATE")
  val DECIMAL = JdbcType(Types.DECIMAL, "DECIMAL")
  val DISTINCT = JdbcType(Types.DISTINCT, "DISTINCT")
  val DOUBLE = JdbcType(Types.DOUBLE, "DOUBLE")
  val FLOAT = JdbcType(Types.FLOAT, "FLOAT")
  val INTEGER = JdbcType(Types.INTEGER, "INTEGER")
  val JAVA_OBJECT = JdbcType(Types.JAVA_OBJECT, "JAVA_OBJECT")
  val LONGNVARCHAR = JdbcType(Types.LONGNVARCHAR, "LONGNVARCHAR")
  val LONGVARBINARY = JdbcType(Types.LONGVARBINARY, "LONGVARBINARY")
  val LONGVARCHAR  = JdbcType(Types.LONGVARCHAR, "LONGVARCHAR")
  val NCHAR = JdbcType(Types.NCHAR, "NCHAR")
  val NCLOB = JdbcType(Types.NCLOB, "NCLOB")
  val NULL = JdbcType(Types.NULL, "NULL")
  val NUMERIC = JdbcType(Types.NUMERIC, "NUMERIC")
  val NVARCHAR = JdbcType(Types.NVARCHAR, "NVARCHAR")
  val OTHER = JdbcType(Types.OTHER, "OTHER")
  val REAL = JdbcType(Types.REAL, "REAL")
  val REF = JdbcType(Types.REF, "REF")
  val ROWID = JdbcType(Types.ROWID, "ROWID")
  val SMALLINT = JdbcType(Types.SMALLINT, "SMALLINT")
  val SQLXML = JdbcType(Types.SQLXML, "SQLXML")
  val STRUCT = JdbcType(Types.STRUCT, "STRUCT")
  val TIME = JdbcType(Types.TIME, "TIME")
  val TIMESTAMP = JdbcType(Types.TIMESTAMP, "TIMESTAMP")
  val TINYINT = JdbcType(Types.TINYINT, "TINYINT")
  val VARBINARY = JdbcType(Types.VARBINARY, "VARBINARY")
  val VARCHAR = JdbcType(Types.VARCHAR, "VARCHAR")

}


