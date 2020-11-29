// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.syntax.all._
import cats.effect.IO
import doobie._, doobie.implicits._
import org.specs2.mutable.Specification
import shapeless._


class fragmentspec extends Specification {

  import cats.effect.unsafe.implicits.global

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:fragmentspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  "Fragment" should {

    val a = 1
    val b = "two"
    val c = true

    val fra = fr"$a"
    val frb = fr"$b"
    val frc = fr"$c"

    "substitute placeholders properly" in {
      fr"foo $a $b bar".query[HNil].sql must_== "foo ? ? bar "
    }

    "concatenate properly" in {
      (fr"foo" ++ fr"bar $a baz").query[HNil].sql must_== "foo bar ? baz "
    }

    "interpolate fragments properly" in {
      fr"foo ${fr0"bar $a baz"}".query[HNil].sql must_== "foo bar ? baz "
    }

    // https://github.com/tpolecat/doobie/issues/1186
    "interpolate an expression `Option(1).getOrElse(2)` properly" in {
      sql"${Option(1).getOrElse(2)} ${false} ${"xx"}"
      fr"${Option(1).getOrElse(2)}"
      fr0"${Option(1).getOrElse(2)}"
      true
    }

    "maintain parameter indexing (in-order)" in {
      val s = fr"select" ++ List(fra, frb, frc).intercalate(fr",")
      s.query[(Int, String, Boolean)].unique.transact(xa).unsafeRunSync() must_== ((a, b, c))
    }

    "maintain parameter indexing (out-of-order)" in {
      val s = fr"select" ++ List(frb, frc, fra).intercalate(fr",")
      s.query[(String, Boolean, Int)].unique.transact(xa).unsafeRunSync() must_== ((b, c, a))
    }

    "maintain associativity (left)" in {
      val s = fr"select" ++ List(fra, fr",", frb, fr",", frc).foldLeft(Fragment.empty)(_ ++ _)
      s.query[(Int, String, Boolean)].unique.transact(xa).unsafeRunSync() must_== ((a, b, c))
    }

    "maintain associativity (right)" in {
      val s = fr"select" ++ List(fra, fr",", frb, fr",", frc).foldRight(Fragment.empty)(_ ++ _)
      s.query[(Int, String, Boolean)].unique.transact(xa).unsafeRunSync() must_== ((a, b, c))
    }

    "Add a trailing space when constructed with .const" in {
      Fragment.const("foo").query[Int].sql must_== "foo "
    }

    "Not add a trailing space when constructed with .const0" in {
      Fragment.const0("foo").query[Int].sql must_== "foo"
    }

    "allow margin stripping" in {
      fr"""select foo
          |  from bar
          |  where a = $a and b = $b and c = $c
          |""".stripMargin.query[Int].sql must_== "select foo\n  from bar\n  where a = ? and b = ? and c = ?\n "
    }

    "allow margin stripping with custom margin" in {
      fr"""select foo
          !  from bar
          !""".stripMargin('!').query[Int].sql must_== "select foo\n  from bar\n "
    }

    "not affect margin characters in middle outside of margin position" in {
      fr"""select foo || baz
          |  from bar
          |""".stripMargin.query[Int].sql must_== "select foo || baz\n  from bar\n "
    }

    // A fragment composed of this many sub-fragments would not be stacksafe without special
    // handling, which we test below.
    val STACK_UNSAFE_SIZE = 20000

    "be stacksafe (left-associve)" in {
      val frag =
        fr0"SELECT 1 WHERE 1 IN (" ++
        List.fill(STACK_UNSAFE_SIZE)(1).foldLeft(Fragment.empty)((f, n) => f ++ fr"$n,") ++
        fr0"1)"
      frag.query[Int].unique.transact(xa).unsafeRunSync() must_== 1
    }

    "be stacksafe (right-associve)" in {
      val frag =
        fr0"SELECT 1 WHERE 1 IN (" ++
        List.fill(STACK_UNSAFE_SIZE)(1).foldRight(Fragment.empty)((n, f) => f ++ fr"$n,") ++
        fr0"1)"
      frag.query[Int].unique.transact(xa).unsafeRunSync() must_== 1
    }

  }

}
