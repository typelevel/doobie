package doobie
package std

import doobie._

object int {

  def apply(j: JdbcType): Primitive[Int] =
    Primitive[Int](j, _.setInt, _.getInt)

  val bigint   = apply(JdbcType.BIGINT)
  val bit      = apply(JdbcType.BIT)
  val decimal  = apply(JdbcType.DECIMAL)
  val integer  = apply(JdbcType.INTEGER)
  val numeric  = apply(JdbcType.NUMERIC)
  val smallint = apply(JdbcType.SMALLINT)
  val tinyint  = apply(JdbcType.TINYINT)

  val default = decimal

}
