package doobie
package dbc

import scalaz.effect.IO
import java.sql

// TODO: enum usage
trait ParameterMetaDataFunctions extends DWorld[sql.ParameterMetaData] {

  def getParameterClassName(param: Int): ParameterMetaData[String] =
    primitive(s"getParameterClassName($param)", _.getParameterClassName(param))

  def getParameterCount: ParameterMetaData[Int] =
    primitive(s"getParameterCount", _.getParameterCount )

  def getParameterMode(param: Int): ParameterMetaData[Int] =
    primitive(s"getParameterMode($param)", _.getParameterMode(param))

  def getParameterType(param: Int): ParameterMetaData[Int] =
    primitive(s"getParameterType($param)", _.getParameterType(param))

  def getParameterTypeName(param: Int): ParameterMetaData[String] =
    primitive(s"getParameterTypeName($param)", _.getParameterTypeName(param))

  def getPrecision(param: Int): ParameterMetaData[Int] =
    primitive(s"getPrecision($param)", _.getPrecision(param))

  def getScale(param: Int): ParameterMetaData[Int] =
    primitive(s"getScale($param)", _.getScale(param))

  def isNullable(param: Int): ParameterMetaData[Int] =
    primitive(s"isNullable($param)", _.isNullable(param))

  def isSigned(param: Int): ParameterMetaData[Boolean] =
    primitive(s"isSigned($param)", _.isSigned(param))

}