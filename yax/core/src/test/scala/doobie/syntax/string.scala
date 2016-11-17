package doobie.syntax

#+scalaz
import scalaz.NonEmptyList
#-scalaz
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

#+scalaz
    "support sequence params" in {
      val a = NonEmptyList(1,2,3)
      implicit val pa = Param.many(a)
      val q = sql"foo ${a : a.type} bar baz".query[Int]
      q.sql must_=== "foo ?, ?, ? bar baz"
    }

    "support multiple distinct sequence params" in {
      val a = NonEmptyList(1,2,3)
      val b = NonEmptyList("foo", "bar")
      implicit val pa = Param.many(a)
      implicit val pb = Param.many(b)
      val q = sql"foo ${a : a.type} bar ${b : b.type} baz".query[Int]
      q.sql must_=== "foo ?, ?, ? bar ?, ? baz"
    }

    "support a combination of atomic and sequence params" in {
      val a = NonEmptyList(1,2,3)
      val b = NonEmptyList("foo", "bar")
      implicit val pa = Param.many(a)
      implicit val pb = Param.many(b)
      val c = 42
      val q = sql"foo ${a : a.type} bar ${b : b.type} baz $c".query[Int]
      q.sql must_=== "foo ?, ?, ? bar ?, ? baz ?"
    }
#-scalaz

    "not support product params" in {
      val a = (1, "two")
      Composite[(Int, String)]
      illTyped(""" sql"foo $a bar baz".query[Int] """)
      true
    }

  }

}
