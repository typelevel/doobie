package doobie

import scalaz.Functor
import scalaz.Contravariant
import doobie.world.connection._

case class Statement[I: Composite, O: Composite](s: String) {

  def apply(i:I) =
    Composite[I].set(i).lift(s)

}

object Statement {

  implicit def functor[I]: Functor[({type l[a] = Statement[I,a]})#l] = 
    ???

  implicit def contravariant[O]: Contravariant[({type l[a] = Statement[a,O]})#l] = 
    ???

}

