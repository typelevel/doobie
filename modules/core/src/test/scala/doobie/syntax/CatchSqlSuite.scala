// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.syntax

import cats.syntax.all.*
import org.typelevel.doobie.*
import org.typelevel.doobie.implicits.*

class CatchSqlSuite extends munit.FunSuite {

  test("CatchSql syntax should work on aliased ConnectionIO") {
    42.pure[ConnectionIO].attemptSql
  }

  test("CatchSql syntax should work on unaliased ConnectionIO") {
    42.pure[ConnectionIO].map(_ + 1).attemptSql
  }

}
