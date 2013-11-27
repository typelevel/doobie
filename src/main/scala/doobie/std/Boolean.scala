package doobie
package std

import doobie.world.statement._
import doobie.world.resultset._
import doobie._

import scalaz.syntax.std.boolean._

object boolean {
  
  /** Map boolean as strings `yes` and `no`. */
  def asString[J: JdbcType](yes: String, no: String): InOut[Boolean, J] =
    string[J].xmap({ case `yes` => true; case `no` => false }, _.fold(yes, no))

  /** Map boolean as an integer. */
  def asInt[J: JdbcType](yes: Int, no: Int): InOut[Boolean, J] =
    int[J].xmap({ case `yes` => true; case `no` => false }, _.fold(yes, no))

  /** Map boolean as `0` or `1`. */
  def asZeroOrOne[J: JdbcType] = asInt(0, 1)

}