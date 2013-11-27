package doobie
package world

import scalaz._
import Scalaz._

import doobie.util.RWSFWorld

// All doobie worlds have Log as their writer
trait DWorld extends RWSFWorld {
  type W = Log
  lazy val W: Monoid[Log] = implicitly
}

object DWorld {

  trait Stateless extends DWorld {
    type S = Unit
  }

}