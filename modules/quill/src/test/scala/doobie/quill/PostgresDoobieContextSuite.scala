// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import cats.effect._
import cats.syntax.all._
import doobie._
import doobie.implicits._
import doobie.quill.DoobieContext
import io.getquill._

class PostgresDoobieContextSuite extends munit.FunSuite {

  // Logging should appear in test output
  sys.props.put("quill.binds.log", "true")
  sys.props.put("org.slf4j.simpleLogger.defaultLogLevel", "debug")

  import cats.effect.unsafe.implicits.global

  // A transactor that always rolls back.
  lazy val xa =
    Transactor.after.set(
      Transactor.fromDriverManager[IO](
        "org.postgresql.Driver",
        "jdbc:postgresql:world",
        "postgres", ""
      ), HC.rollback
    )

  val dc = new DoobieContext.Postgres(Literal)
  import dc._

  case class Country(code: String, name: String, population: Int)

  test("executeQuery should correctly select a country") {
    val stmt     = quote { query[Country].filter(_.code == "GBR") }
    val actual   = run(stmt).transact(xa).unsafeRunSync()
    val expected = List(Country("GBR", "United Kingdom", 59623400))
    assertEquals(actual, expected)
  }

  test("executeQuerySingle should correctly select a constant") {
    val stmt     = quote(42)
    val actual   = run(stmt).transact(xa).unsafeRunSync()
    val expected = 42
    assertEquals(actual, expected)
  }

  test("streamQuery should correctly stream a bunch of countries") {
    val stmt     = quote { query[Country] }
    val actual   = dc.stream(stmt, 16).transact(xa).as(1).compile.foldMonoid.unsafeRunSync()
    val expected = 239 // this many countries total
    assertEquals(actual, expected)
  }

  test("executeAction should correctly update a bunch of countries") {
    val stmt     = quote { query[Country].filter(_.name like "U%").update(_.name -> "foo") }
    val actual   = run(stmt).transact(xa).unsafeRunSync()
    val expected = 8L // this many countries start with 'U'
    assertEquals(actual, expected)
  }

  test("executeBatchAction should correctly do multiple updates") {
    val stmt = quote {
      liftQuery(List("U%", "A%")).foreach { pat =>
        query[Country].filter(_.name like pat).update(_.name -> "foo")
      }
    }
    val actual   = run(stmt).transact(xa).unsafeRunSync()
    val expected = List(8L, 15L)
    assertEquals(actual, expected)
  }

  // For these last two we need a new table with an auto-generated id, so we'll do a temp table.
  val create: ConnectionIO[Unit] =
    sql"""
      CREATE TEMPORARY TABLE QuillTest (
        id    SERIAL,
        value VARCHAR(42)
      ) ON COMMIT DROP
    """.update.run.void

  case class QuillTest(id: Int, value: String)

  test("executeActionReturning should correctly retrieve a generated key") {
    val stmt     = quote { query[QuillTest].insert(lift(QuillTest(0, "Joe"))).returningGenerated(_.id) }
    val actual   = (create *> run(stmt)).transact(xa).unsafeRunSync()
    val expected = 1
    assertEquals(actual, expected)
  }

  test("executeBatchActionReturning should correctly retrieve a list of generated keys") {
    val values   = List(QuillTest(0, "Foo"), QuillTest(0, "Bar"), QuillTest(0, "Baz"))
    val stmt     = quote { liftQuery(values).foreach { a => query[QuillTest].insert(a).returningGenerated(_.id) } }
    val actual   = (create *> run(stmt)).transact(xa).unsafeRunSync()
    val expected = List(1, 2, 3)
    assertEquals(actual, expected)
  }

}