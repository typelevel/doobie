package doobie.syntax

import scalaz._, Scalaz._
import doobie.imports._
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
