package doobie
package std

import doobie._

import scalaz.syntax.std.boolean._

object bigdecimal {

  val numeric = javastd.bigdecimal.numeric.xmap[BigDecimal](BigDecimal(_), _.bigDecimal)

  val default = numeric

}

