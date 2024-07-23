// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*

class SyntaxSuite extends munit.FunSuite {

  test("syntax should not overflow the stack on direct recursion") {
    def prog: ConnectionIO[Unit] = FC.delay(()).onUniqueViolation(prog)
    prog
  }

}
