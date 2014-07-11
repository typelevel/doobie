package doobie.dbc
package op

import enum._
import scalaz.effect.IO
import java.sql

// TODO: enum usage
trait ResultSetMetaDataOps extends PrimitiveOps[sql.ResultSetMetaData] {

  def getCatalogName(column: Int): ResultSetMetaData[String] =
    primitive(s"getCatalogName($column)", _.getCatalogName(column))

  def getColumnClassName(column: Int): ResultSetMetaData[String] =
    primitive(s"getColumnClassName($column)", _.getColumnClassName(column))

  def getColumnCount: ResultSetMetaData[Int] =
    primitive(s"getColumnCount", _.getColumnCount )

  def getColumnDisplaySize(column: Int): ResultSetMetaData[Int] =
    primitive(s"getColumnDisplaySize($column)", _.getColumnDisplaySize(column))

  def getColumnLabel(column: Int): ResultSetMetaData[String] =
    primitive(s"getColumnLabel($column)", _.getColumnLabel(column))

  def getColumnName(column: Int): ResultSetMetaData[String] =
    primitive(s"getColumnName($column)", _.getColumnName(column))

  def getColumnType(column: Int): ResultSetMetaData[Int] =
    primitive(s"getColumnType($column)", _.getColumnType(column))

  def getColumnTypeName(column: Int): ResultSetMetaData[String] =
    primitive(s"getColumnTypeName($column)", _.getColumnTypeName(column))

  def getPrecision(column: Int): ResultSetMetaData[Int] =
    primitive(s"getPrecision($column)", _.getPrecision(column))

  def getScale(column: Int): ResultSetMetaData[Int] =
    primitive(s"getScale($column)", _.getScale(column))

  def getSchemaName(column: Int): ResultSetMetaData[String] =
    primitive(s"getSchemaName($column)", _.getSchemaName(column))

  def getTableName(column: Int): ResultSetMetaData[String] =
    primitive(s"getTableName($column)", _.getTableName(column))

  def isAutoIncrement(column: Int): ResultSetMetaData[Boolean] =
    primitive(s"isAutoIncrement($column)", _.isAutoIncrement(column))

  def isCaseSensitive(column: Int): ResultSetMetaData[Boolean] =
    primitive(s"isCaseSensitive($column)", _.isCaseSensitive(column))

  def isCurrency(column: Int): ResultSetMetaData[Boolean] =
    primitive(s"isCurrency($column)", _.isCurrency(column))

  def isDefinitelyWritable(column: Int): ResultSetMetaData[Boolean] =
    primitive(s"isDefinitelyWritable($column)", _.isDefinitelyWritable(column))

  def isNullable(column: Int): ResultSetMetaData[Int] =
    primitive(s"isNullable($column)", _.isNullable(column))

  def isReadOnly(column: Int): ResultSetMetaData[Boolean] =
    primitive(s"isReadOnly($column)", _.isReadOnly(column))

  def isSearchable(column: Int): ResultSetMetaData[Boolean] =
    primitive(s"isSearchable($column)", _.isSearchable(column))

  def isSigned(column: Int): ResultSetMetaData[Boolean] =
    primitive(s"isSigned($column)", _.isSigned(column))

  def isWritable(column: Int): ResultSetMetaData[Boolean] =
    primitive(s"isWritable($column)", _.isWritable(column))

}