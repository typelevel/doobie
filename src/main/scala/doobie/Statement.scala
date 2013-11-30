package doobie

import scalaz.Functor
import scalaz.Contravariant
import doobie.world._
import doobie.world.connection._

import scalaz._
import Scalaz._

case class Query[I, O: Composite](s: String)(implicit I : Composite[I]) {
  def apply(i:I) = (I.set(i) >> resultset.list[O].lift).lift(s)
}

case class Query0[O: Composite](s: String) {
  def apply = resultset.list[O].lift.lift(s)
}

