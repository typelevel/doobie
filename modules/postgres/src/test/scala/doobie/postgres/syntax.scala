package doobie.postgres

import doobie._, doobie.implicits._
import doobie.postgres._, doobie.postgres.implicits._
import doobie.postgres.pgistypes._
import org.specs2.mutable.Specification

object syntaxspec extends Specification {

  "syntax" should {

    "not overflow the stack on direct recursion" in {
      def prog: ConnectionIO[Unit] = FC.delay(()).onUniqueViolation(prog)
      prog
      true
    }

  }

}
