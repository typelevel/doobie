package doobie

import scalaz.Functor
import scalaz.Contravariant
import doobie.world.connection._
import doobie.param._

case class Statement[I: InParam, O: OutParam](s: String) {

  def apply(i:I) =
    statement(s, InParam[I].set(i))

}

object Statement {

  implicit def functor[I]: Functor[({type l[a] = Statement[I,a]})#l] = 
    ???

  implicit def contravariant[O]: Contravariant[({type l[a] = Statement[a,O]})#l] = 
    ???

}