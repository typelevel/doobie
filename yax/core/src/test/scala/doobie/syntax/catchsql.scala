package doobie.syntax

#+scalaz
import scalaz._, Scalaz._
#-scalaz
#+cats
import cats.implicits._
import fs2.interop.cats._
#-cats
import doobie.imports._
import shapeless.test.illTyped
import org.specs2.mutable.Specification

object catchsqlspec extends Specification {

  "catchsql syntax" should {

    "work on aliased IConnection" in {
      42.pure[ConnectionIO].attemptSql
      true
    }

    "work on unaliased IConnection" in {
      42.pure[ConnectionIO].map(_ + 1).attemptSql
      true
    }

  }

}
