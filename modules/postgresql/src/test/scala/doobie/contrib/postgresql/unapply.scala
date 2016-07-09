package doobie.util

import shapeless._, shapeless.test._
import doobie.imports._
import doobie.contrib.postgresql.syntax._
import org.specs2.mutable.Specification

import scalaz.{ Free, Coyoneda, Monad }
import scalaz.syntax.monad._

object unapplyspec extends Specification {

  "Unapply" should { 

    "allow use of sqlstate syntax" in {
      1.point[ConnectionIO].map(_ + 1).void
      1.point[ConnectionIO].map(_ + 1).onPrivilegeNotRevoked(2.point[ConnectionIO])
      true
    }

  }

}
