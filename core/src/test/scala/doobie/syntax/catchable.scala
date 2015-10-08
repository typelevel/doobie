package doobie.syntax

import scalaz._, Scalaz._
import doobie.imports._
import shapeless.test.illTyped
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