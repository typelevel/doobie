package doobie.util

import shapeless._, shapeless.test._
import doobie.imports._
import org.specs2.mutable.Specification

#+scalaz
import scalaz.{ Free, Coyoneda, Monad }
#-scalaz
#+cats
import cats.Monad
import cats.free.{ Free, Coyoneda }
#-cats

object unapplyspec extends Specification {

  "Partial Unification" should { 

    "allow inference of Monad[Free[Coyoneda[F, ?], ?]]" in {
      trait Foo[A]
      Monad[Free[Coyoneda[Foo, ?], ?]]
      true
    }

  }

}
