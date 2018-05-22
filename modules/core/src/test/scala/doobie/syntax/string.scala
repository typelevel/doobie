// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.syntax

import doobie._, doobie.implicits._
import shapeless.test.illTyped
import org.specs2.mutable.Specification

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object stringspec extends Specification {

  "sql interpolator" should {

    "support no-param queries" in {
      val q = sql"foo bar baz".query[Int]
      q.sql must_=== "foo bar baz"
    }

    "support atomic types" in {
      val a = 1
      val b = "two"
      val q = sql"foo $a bar $b baz".query[Int]
      q.sql must_=== "foo ? bar ? baz"
    }

    "handle leading params" in {
      val a = 1
      val q = sql"$a bar baz".query[Int]
      q.sql must_=== "? bar baz"
    }

    "support trailing params" in {
      val b = "two"
      val q = sql"foo bar $b".query[Int]
      q.sql must_=== "foo bar ?"
    }

    "not support product params" in {
      Write[(Int, String)]
      illTyped(""" val a = (1, "two"); sql"foo $a bar baz".query[Int] """)
      true
    }

  }

}
