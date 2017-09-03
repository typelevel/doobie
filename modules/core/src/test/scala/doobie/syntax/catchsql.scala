package doobie.syntax

import cats.implicits._
import doobie._, doobie.implicits._
import org.specs2.mutable.Specification

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
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
