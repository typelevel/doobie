package doobie.util

import shapeless._, shapeless.test._
import doobie.imports._
import org.specs2.mutable.Specification

import scalaz.{ Free, Coyoneda, Monad }

object unapplyspec extends Specification {

  "Unapply" should { 

    "allow inference of Monad[Free[Coyoneda[F, ?], ?]]" in {
      trait Foo[A]
      Monad[Free[Coyoneda[Foo, ?], ?]]
      true
    }

  }

}
