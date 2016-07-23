package doobie.syntax

#+scalaz
import scalaz.syntax.monad._
import scalaz.syntax.catchable._
#-scalaz
#+cats
import doobie.util.catchable._
import cats.implicits._
#-cats

import doobie.imports._
import org.specs2.mutable.Specification

object catchablespec extends Specification {

  "catchable syntax" should {

    "work on aliased IConnection" in {
      42.pure[ConnectionIO].attempt
      true
    }

    "work on unaliased IConnection" in {
      42.pure[ConnectionIO].map(_ + 1).attempt
      true
    }

  }

}