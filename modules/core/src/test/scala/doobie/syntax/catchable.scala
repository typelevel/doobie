package doobie.syntax

import fs2.interop.cats._
import cats.implicits._

import doobie.imports._
import org.specs2.mutable.Specification

object catchablespec extends Specification {

  "catchable syntax" should {

    "work on aliased IConnection" in {
      42.pure[ConnectionIO].attempt
      true
    }

// TODO Remove yax (https://github.com/tpolecat/doobie/issues/369)

  }

}
