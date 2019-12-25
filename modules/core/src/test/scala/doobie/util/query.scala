// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.{ ContextShift, IO }
import doobie._, doobie.implicits._
import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext
import scala.Predef._


class queryspec extends Specification {

  implicit def contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  val q = Query[String,Int]("select 123 where ? = 'foo'", None)
  val pairQuery = Query[String, (String, Int)]("select 'xxx', 123 where ? = 'foo'", None)

  "Query (non-empty)" >> {
    "to" in {
      q.to[List]("foo").transact(xa).unsafeRunSync must_=== List(123)
    }
    "toMap" in {
      pairQuery.toMap("foo").transact(xa).unsafeRunSync must_=== Map("xxx" -> 123)
    }
    "unique" in {
      q.unique("foo").transact(xa).unsafeRunSync must_=== 123
    }
    "option" in {
      q.option("foo").transact(xa).unsafeRunSync must_=== Some(123)
    }
    "map" in {
      q.map("x" * _).to[List]("foo").transact(xa).unsafeRunSync must_=== List("x" * 123)
    }
    "contramap" in {
      q.contramap[Int](n => "foo" * n).to[List](1).transact(xa).unsafeRunSync must_=== List(123)
    }
  }

  "Query (empty)" >> {
    "to" in {
      q.to[List]("bar").transact(xa).unsafeRunSync must_=== Nil
    }
    "toMap" in {
      pairQuery.toMap("bar").transact(xa).unsafeRunSync must_=== Map.empty
    }
    "unique" in {
      q.unique("bar").transact(xa).attempt.unsafeRunSync must_=== Left(invariant.UnexpectedEnd)
    }
    "option" in {
      q.option("bar").transact(xa).unsafeRunSync must_=== None
    }
    "map" in {
      q.map("x" * _).to[List]("bar").transact(xa).unsafeRunSync must_=== Nil
    }
    "contramap" in {
      q.contramap[Int](n => "bar" * n).to[List](1).transact(xa).unsafeRunSync must_=== Nil
    }
  }

  "Query0 from Query (non-empty)" >> {
    "to" in {
      q.toQuery0("foo").to[List].transact(xa).unsafeRunSync must_=== List(123)
    }
    "toMap" in {
      pairQuery.toQuery0("foo").toMap.transact(xa).unsafeRunSync must_=== Map("xxx" -> 123)
    }
    "unique" in {
      q.toQuery0("foo").unique.transact(xa).unsafeRunSync must_=== 123
    }
    "option" in {
      q.toQuery0("foo").option.transact(xa).unsafeRunSync must_=== Some(123)
    }
    "map" in {
      q.toQuery0("foo").map(_ * 2).to[List].transact(xa).unsafeRunSync must_=== List(246)
    }
  }

  "Query0 from Query (empty)" >> {
    "to" in {
      q.toQuery0("bar").to[List].transact(xa).unsafeRunSync must_=== Nil
    }
    "toMap" in {
      pairQuery.toQuery0("bar").toMap.transact(xa).unsafeRunSync must_=== Map.empty
    }
    "unique" in {
      q.toQuery0("bar").unique.transact(xa).attempt.unsafeRunSync must_=== Left(invariant.UnexpectedEnd)
    }
    "option" in {
      q.toQuery0("bar").option.transact(xa).unsafeRunSync must_=== None
    }
    "map" in {
      q.toQuery0("bar").map(_ * 2).to[List].transact(xa).unsafeRunSync must_=== Nil
    }
  }

  val q0n = Query0[Int]("select 123 where 'foo' = 'foo'", None)
  val pairQ0n = Query0[(String, Int)]("select 'xxx', 123 where 'foo' = 'foo'", None)

  "Query0 via constructor (non-empty)" >> {
    "to" in {
      q0n.to[List].transact(xa).unsafeRunSync must_=== List(123)
    }
    "toMap" in {
      pairQ0n.toMap.transact(xa).unsafeRunSync must_=== Map("xxx" -> 123)
    }
    "unique" in {
      q0n.unique.transact(xa).unsafeRunSync must_=== 123
    }
    "option" in {
      q0n.option.transact(xa).unsafeRunSync must_=== Some(123)
    }
    "map" in {
      q0n.map(_ * 2).to[List].transact(xa).unsafeRunSync must_=== List(246)
    }
  }

  val q0e = Query0[Int]("select 123 where 'bar' = 'foo'", None)
  val pairQ0e = Query0[(String, Int)]("select 'xxx', 123 where 'bar' = 'foo'", None)

  "Query0 via constructor (empty)" >> {
    "to" in {
      q0e.to[List].transact(xa).unsafeRunSync must_=== Nil
    }
    "toMap" in {
      pairQ0e.toMap.transact(xa).unsafeRunSync must_=== Map.empty
    }
    "unique" in {
      q0e.unique.transact(xa).attempt.unsafeRunSync must_=== Left(invariant.UnexpectedEnd)
    }
    "option" in {
      q0e.option.transact(xa).unsafeRunSync must_=== None
    }
    "map" in {
      q0e.map(_ * 2).to[List].transact(xa).unsafeRunSync must_=== Nil
    }
  }

  val qf = sql"select 'foo', ${1:Int}, ${Option.empty[Int]}, ${Option(42)}".query[String] // wrong!
  "Query to Fragment and back" >> {
    "test" in {
      val qfʹ = qf.toFragment.query[(String, Int, Option[Int], Option[Int])]
      qfʹ.unique.transact(xa).unsafeRunSync must_=== (("foo", 1, None, Some(42)))
    }
  }

}
