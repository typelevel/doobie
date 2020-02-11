// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.syntax

import cats.implicits._
import doobie._, doobie.implicits._
import org.specs2.mutable.Specification


class catchsqlspec extends Specification {

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
