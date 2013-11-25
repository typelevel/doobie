package doobie
package world

import scalaz._
import doobie.util.RWSFWorld
import scalaz._
import Scalaz._

// All doobie worlds have Log as their writer
trait DWorld extends RWSFWorld {
  type W = Log
  lazy val W: Monoid[Log] = implicitly
}