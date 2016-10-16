package doobie.syntax

#+scalaz
import scalaz.syntax.monad._
import scalaz.syntax.catchable._
#-scalaz
#+cats
#+fs2
import fs2.interop.cats._
#-fs2
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
#+scalaz
      42.pure[ConnectionIO].map(_ + 1).attempt
#-scalaz
#+cats
      // TODO Remove yax (https://github.com/tpolecat/doobie/issues/369)
      (42.pure[ConnectionIO].map(_ + 1): ConnectionIO[Int]).attempt
#-cats
      true
    }

  }

}
