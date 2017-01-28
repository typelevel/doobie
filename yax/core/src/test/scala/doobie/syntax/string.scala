package doobie.syntax

#+scalaz
import scalaz.NonEmptyList
#-scalaz
#+cats
import fs2.interop.cats._
#-cats

import doobie.imports._
import shapeless._
import shapeless.test.illTyped
import org.specs2.mutable.Specification

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
      val a = (1, "two")
      Composite[(Int, String)]
      illTyped(""" sql"foo $a bar baz".query[Int] """)
      true
    }

  }

}
