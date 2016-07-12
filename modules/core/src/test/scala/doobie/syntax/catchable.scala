package doobie.syntax

import scalaz.syntax.monad._
import scalaz.syntax.catchable._

import doobie.imports._
import org.specs2.mutable.Specification

object catchablespec extends Specification {

  "catchable syntax" should {

    "work on aliased IConnection" in {
      42.point[ConnectionIO].attempt
      true
    }

    "work on unaliased IConnection" in {
      42.point[ConnectionIO].map(_ + 1).attempt
      true
    }

  }

}