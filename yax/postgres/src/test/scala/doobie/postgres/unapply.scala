package doobie.postgres

import doobie.imports._
import doobie.postgres.imports._

import shapeless._, shapeless.test._
import org.specs2.mutable.Specification

#+scalaz
import scalaz.syntax.monad._
#-scalaz
#+cats
import cats.implicits._
#-cats

object unapplyspec extends Specification {

  "Partial Unification" should {

    "allow use of sqlstate syntax" in {
      1.pure[ConnectionIO].map(_ + 1).void
      1.pure[ConnectionIO].map(_ + 1).onPrivilegeNotRevoked(2.pure[ConnectionIO])
      true
    }

  }

}
