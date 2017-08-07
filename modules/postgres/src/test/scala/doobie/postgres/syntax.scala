package doobie.postgres

import doobie.imports._
import doobie.postgres.imports._
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
