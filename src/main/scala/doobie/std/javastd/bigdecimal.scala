package doobie
package std
package javastd

import doobie._

import scalaz.syntax.std.boolean._
import java.math.{ BigDecimal => JBigDecimal }

object bigdecimal {

  val numeric = Primitive[JBigDecimal](JdbcType.NUMERIC, _.setBigDecimal, _.getBigDecimal)
  
  val default = numeric

}

