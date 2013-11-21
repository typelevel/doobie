package doobie

import scalaz.Functor
import scalaz.Contravariant

case class Statement[I: Param, O: Param](s: String) {

}

object Statement {

  implicit def functor[I]: Functor[({type l[a] = Statement[I,a]})#l] = 
    ???

  implicit def contravariant[O]: Contravariant[({type l[a] = Statement[a,O]})#l] = 
    ???

}