package doobie
package std

import doobie.world.StatementWorld._
import doobie.world.ResultSetWorld._
import doobie._

object int {

  def apply[J: JdbcType]: InOut[Int, J] =
    InOut(In(_.setInt), Out(_.getInt))

  val bigint   = apply[JdbcType.BIGINT]
  val bit      = apply[JdbcType.BIT]
  val decimal  = apply[JdbcType.DECIMAL]
  val integer  = apply[JdbcType.INTEGER]
  val numeric  = apply[JdbcType.NUMERIC]
  val smallint = apply[JdbcType.SMALLINT]
  val tinyint  = apply[JdbcType.TINYINT]

}

