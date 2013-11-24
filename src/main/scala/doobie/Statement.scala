package doobie

import scalaz.Functor
import scalaz.Contravariant
import world.ConnectionWorld._

case class Statement[I: Param, O: Param](s: String) {

  def apply(i:I) =
    statement(s, Param[I].set(i))

}

object Statement {

  implicit def functor[I]: Functor[({type l[a] = Statement[I,a]})#l] = 
    ???

  implicit def contravariant[O]: Contravariant[({type l[a] = Statement[a,O]})#l] = 
    ???

}