package doobie

import java.sql.Types
import scalaz._
import Scalaz._


case class JdbcType[A](toInt: Int, name: String)

object JdbcType {

  trait ARRAY
  object ARRAY {
    implicit val jdbcType: JdbcType[ARRAY] =
      JdbcType(Types.ARRAY, "ARRAY")
  }

  trait BIGINT
  object BIGINT {
    implicit val jdbcType: JdbcType[BIGINT] =
      JdbcType(Types.BIGINT, "BIGINT")
  }

  trait BINARY
  object BINARY {
    implicit val jdbcType: JdbcType[BINARY] =
      JdbcType(Types.BINARY, "BINARY")
  }

  trait BIT
  object BIT {
    implicit val jdbcType: JdbcType[BIT] =
      JdbcType(Types.BIT, "BIT")
  }

  trait BLOB
  object BLOB {
    implicit val jdbcType: JdbcType[BLOB] =
      JdbcType(Types.BLOB, "BLOB")
  }

  trait BOOLEAN
  object BOOLEAN {
    implicit val jdbcType: JdbcType[BOOLEAN] =
      JdbcType(Types.BOOLEAN, "BOOLEAN")
  }

  trait CHAR
  object CHAR {
    implicit val jdbcType: JdbcType[CHAR] =
      JdbcType(Types.CHAR, "CHAR")
  }

  trait CLOB
  object CLOB {
    implicit val jdbcType: JdbcType[CLOB] =
      JdbcType(Types.CLOB, "CLOB")
  }

  trait DATALINK
  object DATALINK {
    implicit val jdbcType: JdbcType[DATALINK] =
      JdbcType(Types.DATALINK, "DATALINK")
  }

  trait DATE
  object DATE {
    implicit val jdbcType: JdbcType[DATE] =
      JdbcType(Types.DATE, "DATE")
  }

  trait DECIMAL
  object DECIMAL {
    implicit val jdbcType: JdbcType[DECIMAL] =
      JdbcType(Types.DECIMAL, "DECIMAL")
  }

  trait DISTINCT
  object DISTINCT {
    implicit val jdbcType: JdbcType[DISTINCT] =
      JdbcType(Types.DISTINCT, "DISTINCT")
  }

  trait DOUBLE
  object DOUBLE {
    implicit val jdbcType: JdbcType[DOUBLE] =
      JdbcType(Types.DOUBLE, "DOUBLE")
  }

  trait FLOAT
  object FLOAT {
    implicit val jdbcType: JdbcType[FLOAT] =
      JdbcType(Types.FLOAT, "FLOAT")
  }

  trait INTEGER
  object INTEGER {
    implicit val jdbcType: JdbcType[INTEGER] =
      JdbcType(Types.INTEGER, "INTEGER")
  }

  trait JAVA_OBJECT
  object JAVA_OBJECT {
    implicit val jdbcType: JdbcType[JAVA_OBJECT] =
      JdbcType(Types.JAVA_OBJECT, "JAVA_OBJECT")
  }

  trait LONGNVARCHAR
  object LONGNVARCHAR {
    implicit val jdbcType: JdbcType[LONGNVARCHAR] =
      JdbcType(Types.LONGNVARCHAR, "LONGNVARCHAR")
  }

  trait LONGVARBINARY
  object LONGVARBINARY {
    implicit val jdbcType: JdbcType[LONGVARBINARY] =
      JdbcType(Types.LONGVARBINARY, "LONGVARBINARY")
  }

  trait LONGVARCHAR
  object LONGVARCHAR {
    implicit val jdbcType: JdbcType[LONGVARCHAR] =
      JdbcType(Types.LONGVARCHAR, "LONGVARCHAR")
  }

  trait NCHAR
  object NCHAR {
    implicit val jdbcType: JdbcType[NCHAR] =
      JdbcType(Types.NCHAR, "NCHAR")
  }

  trait NCLOB
  object NCLOB {
    implicit val jdbcType: JdbcType[NCLOB] =
      JdbcType(Types.NCLOB, "NCLOB")
  }

  trait NULL
  object NULL {
    implicit val jdbcType: JdbcType[NULL] =
      JdbcType(Types.NULL, "NULL")
  }

  trait NUMERIC
  object NUMERIC {
    implicit val jdbcType: JdbcType[NUMERIC] =
      JdbcType(Types.NUMERIC, "NUMERIC")
  }

  trait NVARCHAR
  object NVARCHAR {
    implicit val jdbcType: JdbcType[NVARCHAR] =
      JdbcType(Types.NVARCHAR, "NVARCHAR")
  }

  trait OTHER
  object OTHER {
    implicit val jdbcType: JdbcType[OTHER] =
      JdbcType(Types.OTHER, "OTHER")
  }

  trait REAL
  object REAL {
    implicit val jdbcType: JdbcType[REAL] =
      JdbcType(Types.REAL, "REAL")
  }

  trait REF
  object REF {
    implicit val jdbcType: JdbcType[REF] =
      JdbcType(Types.REF, "REF")
  }

  trait ROWID
  object ROWID {
    implicit val jdbcType: JdbcType[ROWID] =
      JdbcType(Types.ROWID, "ROWID")
  }

  trait SMALLINT
  object SMALLINT {
    implicit val jdbcType: JdbcType[SMALLINT] =
      JdbcType(Types.SMALLINT, "SMALLINT")
  }

  trait SQLXML
  object SQLXML {
    implicit val jdbcType: JdbcType[SQLXML] =
      JdbcType(Types.SQLXML, "SQLXML")
  }

  trait STRUCT
  object STRUCT {
    implicit val jdbcType: JdbcType[STRUCT] =
      JdbcType(Types.STRUCT, "STRUCT")
  }

  trait TIME
  object TIME {
    implicit val jdbcType: JdbcType[TIME] =
      JdbcType(Types.TIME, "TIME")
  }

  trait TIMESTAMP
  object TIMESTAMP {
    implicit val jdbcType: JdbcType[TIMESTAMP] =
      JdbcType(Types.TIMESTAMP, "TIMESTAMP")
  }

  trait TINYINT
  object TINYINT {
    implicit val jdbcType: JdbcType[TINYINT] =
      JdbcType(Types.TINYINT, "TINYINT")
  }

  trait VARBINARY
  object VARBINARY {
    implicit val jdbcType: JdbcType[VARBINARY] =
      JdbcType(Types.VARBINARY, "VARBINARY")
  }

  trait VARCHAR
  object VARCHAR {
    implicit val jdbcType: JdbcType[VARCHAR] =
      JdbcType(Types.VARCHAR, "VARCHAR")
  }
}


