package doobie.postgres

import shapeless._, shapeless.test._
import doobie.imports._
import doobie.postgres.syntax._
import org.specs2.mutable.Specification

#+scalaz
import scalaz.syntax.monad._
#-scalaz
#+cats
import cats.implicits._
#-cats

object unapplyspec extends Specification {

  "Unapply" should { 

    "allow use of sqlstate syntax" in {
      1.pure[ConnectionIO].map(_ + 1).void
      1.pure[ConnectionIO].map(_ + 1).onPrivilegeNotRevoked(2.pure[ConnectionIO])
      true
    }

  }

}
