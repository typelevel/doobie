package doobie
package std

import doobie._

object int {

  def apply[J](implicit J: JdbcType[J]): Primitive[Int] =
    new Primitive[Int] {
      def set = _.setInt
      def get = _.getInt
      def jdbcType = J
    }

  val bigint   = apply[JdbcType.BIGINT]
  val bit      = apply[JdbcType.BIT]
  val decimal  = apply[JdbcType.DECIMAL]
  val integer  = apply[JdbcType.INTEGER]
  val numeric  = apply[JdbcType.NUMERIC]
  val smallint = apply[JdbcType.SMALLINT]
  val tinyint  = apply[JdbcType.TINYINT]

}
