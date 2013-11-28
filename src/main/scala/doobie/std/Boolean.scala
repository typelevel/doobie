package doobie
package std

import doobie._
import scalaz.syntax.std.boolean._

object boolean {

  val bit     = Primitive[Boolean](JdbcType.BIT, _.setBoolean, _.getBoolean)
  val boolean = Primitive[Boolean](JdbcType.BOOLEAN, _.setBoolean, _.getBoolean)

  val default = boolean

  //////

  def as[A](trueValue: A, falseValue: A)(implicit A: Primitive[A]): Primitive[Boolean] =
    A.xmap({ 
        case `trueValue`  => true
        case `falseValue` => false
      }, _.fold(trueValue, falseValue))

}

