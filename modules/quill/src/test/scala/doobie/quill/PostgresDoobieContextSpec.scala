// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.quill._
import io.getquill._
import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext

object PostgresDoobieContextSpec extends Specification {

  implicit def contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  // A transactor that always rolls back.
  lazy val xa =
    Transactor.after.set(
      Transactor.fromDriverManager[IO](
        "org.postgresql.Driver",
        "jdbc:postgresql:world",
        "postgres", ""
      ), HC.rollback
    )

  val dc = new PostgresDoobieContext(Literal)
  import dc._

  case class Country(code: String, name: String, population: Int)

  "executeQuery" should {
    "correctly select a country" in {
      val stmt     = quote { query[Country].filter(_.code == "GBR") }
      val actual   = dc.run(stmt).transact(xa).unsafeRunSync
      val expected = List(Country("GBR", "United Kingdom", 59623400))
      actual should_== expected
    }
  }

  "executeQuerySingle" should {
    "correctly select a constant" in {
      val stmt     = quote(42)
      val actual   = dc.run(stmt).transact(xa).unsafeRunSync
      val expected = 42
      actual should_== expected
    }
  }

  "streamQuery" should {
    "correctly stream a bunch of countries" in {
      val stmt     = quote { query[Country] }
      val actual   = dc.stream(stmt, 16).transact(xa).as(1).compile.foldMonoid.unsafeRunSync
      val expected = 239 // this many countries total
      actual should_== expected
    }
  }

  "executeAction" should {
    "correctly update a bunch of countries" in {
      val stmt     = quote { query[Country].filter(_.name like "U%").update(_.name -> "foo") }
      val actual   = dc.run(stmt).transact(xa).unsafeRunSync
      val expected = 8 // this many countries start with 'U'
      actual should_== expected
    }
  }

  "executeActionReturning" should {
    failure("not implemented")
  }

  "executeBatchAction" should {
    "correctly do multiple updates" in {
      val stmt = quote {
        liftQuery(List("U%", "A%")).foreach { pat =>
          query[Country].filter(_.name like pat).update(_.name -> "foo")
        }
      }
      val actual   = dc.run(stmt).transact(xa).unsafeRunSync
      val expected = List(8L, 15L)
      actual should_== expected
    }
  }

  "executeBatchActionReturning" should {
    failure("not implemented")
  }


}