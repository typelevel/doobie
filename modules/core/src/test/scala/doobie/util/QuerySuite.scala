// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.effect.IO
import doobie.*, doobie.implicits.*
import scala.Predef.*

class QuerySuite extends munit.CatsEffectSuite {

  val xa = Transactor.fromDriverManager[IO](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None
  )

  val q = Query[String, Int]("select 123 where ? = 'foo'", None)
  val pairQuery = Query[String, (String, Int)]("select 'xxx', 123 where ? = 'foo'", None)

  test("Query (non-empty) to") {
    q.to[List]("foo").transact(xa) assertEquals (List(123))
  }
  test("Query (non-empty) toMap") {
    pairQuery.toMap[String, Int]("foo").transact(xa) assertEquals (Map("xxx" -> 123))
  }
  test("Query (non-empty) unique") {
    q.unique("foo").transact(xa).assertEquals(123)
  }
  test("Query (non-empty) option") {
    q.option("foo").transact(xa) assertEquals (Some(123))
  }
  test("Query (non-empty) map") {
    q.map("x" * _).to[List]("foo").transact(xa) assertEquals (List("x" * 123))
  }
  test("Query (non-empty) contramap") {
    q.contramap[Int](n => "foo" * n).to[List](1).transact(xa).assertEquals(List(123))
  }

  test("Query (empty) to") {
    q.to[List]("bar").transact(xa).assertEquals(Nil)
  }
  test("Query (empty) toMap") {
    pairQuery.toMap[String, Int]("bar").transact(xa).assertEquals(Map.empty[String, Int])
  }
  test("Query (empty) unique") {
    q.unique("bar").transact(xa).attempt.assertEquals(Left(invariant.UnexpectedEnd))
  }
  test("Query (empty) option") {
    q.option("bar").transact(xa).assertEquals(None)
  }
  test("Query (empty) map") {
    q.map("x" * _).to[List]("bar").transact(xa).assertEquals(Nil)
  }
  test("Query (empty) contramap") {
    q.contramap[Int](n => "bar" * n).to[List](1).transact(xa).assertEquals(Nil)
  }

  test("Query0 from Query (non-empty) to") {
    q.toQuery0("foo").to[List].transact(xa).assertEquals(List(123))
  }
  test("Query0 from Query (non-empty) toMap") {
    pairQuery.toQuery0("foo").toMap[String, Int].transact(xa).assertEquals(Map("xxx" -> 123))
  }
  test("Query0 from Query (non-empty) unique") {
    q.toQuery0("foo").unique.transact(xa).assertEquals(123)
  }
  test("Query0 from Query (non-empty) option") {
    q.toQuery0("foo").option.transact(xa).assertEquals(Some(123))
  }
  test("Query0 from Query (non-empty) map") {
    q.toQuery0("foo").map(_ * 2).to[List].transact(xa).assertEquals(List(246))
  }

  test("Query0 from Query (empty) to") {
    q.toQuery0("bar").to[List].transact(xa).assertEquals(Nil)
  }
  test("Query0 from Query (empty) toMap") {
    pairQuery.toQuery0("bar").toMap[String, Int].transact(xa).assertEquals(Map.empty[String, Int])
  }
  test("Query0 from Query (empty) unique") {
    q.toQuery0("bar").unique.transact(xa).attempt.assertEquals(Left(invariant.UnexpectedEnd))
  }
  test("Query0 from Query (empty) option") {
    q.toQuery0("bar").option.transact(xa).assertEquals(None)
  }
  test("Query0 from Query (empty) map") {
    q.toQuery0("bar").map(_ * 2).to[List].transact(xa).assertEquals(Nil)
  }

  val q0n = Query0[Int]("select 123 where 'foo' = 'foo'", None)
  val pairQ0n = Query0[(String, Int)]("select 'xxx', 123 where 'foo' = 'foo'", None)

  test("Query0 via constructor (non-empty) to") {
    q0n.to[List].transact(xa).assertEquals(List(123))
  }
  test("Query0 via constructor (non-empty) toMap") {
    pairQ0n.toMap[String, Int].transact(xa).assertEquals(Map("xxx" -> 123))
  }
  test("Query0 via constructor (non-empty) unique") {
    q0n.unique.transact(xa).assertEquals(123)
  }
  test("Query0 via constructor (non-empty) option") {
    q0n.option.transact(xa).assertEquals(Some(123))
  }
  test("Query0 via constructor (non-empty) map") {
    q0n.map(_ * 2).to[List].transact(xa).assertEquals(List(246))
  }

  val q0e = Query0[Int]("select 123 where 'bar' = 'foo'", None)
  val pairQ0e = Query0[(String, Int)]("select 'xxx', 123 where 'bar' = 'foo'", None)

  test("Query0 via constructor (empty) to") {
    q0e.to[List].transact(xa).assertEquals(Nil)
  }
  test("Query0 via constructor (empty) toMap") {
    pairQ0e.toMap[String, Int].transact(xa).assertEquals(Map.empty[String, Int])
  }
  test("Query0 via constructor (empty) unique") {
    q0e.unique.transact(xa).attempt.assertEquals(Left(invariant.UnexpectedEnd))
  }
  test("Query0 via constructor (empty) option") {
    q0e.option.transact(xa).assertEquals(None)
  }
  test("Query0 via constructor (empty) map") {
    q0e.map(_ * 2).to[List].transact(xa).assertEquals(Nil)
  }

  val qf = sql"select 'foo', ${1: Int}, ${Option.empty[Int]}, ${Option(42)}".query[String] // wrong!
  test("Query to Fragment and back") {
    val qfʹ = qf.toFragment.query[(String, Int, Option[Int], Option[Int])]
    qfʹ.unique.transact(xa).assertEquals(("foo", 1, None, Some(42)))
  }

}
