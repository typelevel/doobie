package doobie.postgres

import doobie.imports._
import doobie.postgres.imports._
import org.specs2.mutable.Specification

import cats.implicits._

object unapplyspec extends Specification {

  "Partial Unification" should {

    "allow use of sqlstate syntax" in {
      1.pure[ConnectionIO].map(_ + 1).void
      1.pure[ConnectionIO].map(_ + 1).onPrivilegeNotRevoked(2.pure[ConnectionIO])
      true
    }

  }

}
