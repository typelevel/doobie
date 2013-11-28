package doobie
package std

import doobie.world.statement._
import doobie.world.resultset._
import doobie.JdbcType

object string extends string

trait string {

  def apply[J](implicit J: JdbcType[J]): Primitive[String] =
    new Primitive[String] {
      def set = _.setString
      def get = _.getString
      def jdbcType = J
    }

  val char         = apply[JdbcType.CHAR]
  val longnvarchar = apply[JdbcType.LONGNVARCHAR]
  val longvarchar  = apply[JdbcType.LONGVARCHAR]
  val nchar        = apply[JdbcType.NCHAR]
  val nvarchar     = apply[JdbcType.NVARCHAR]
  val varchar      = apply[JdbcType.VARCHAR]

}