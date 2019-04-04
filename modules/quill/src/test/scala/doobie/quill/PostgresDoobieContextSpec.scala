// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import cats.effect._
import doobie._
import doobie.implicits._
import doobie.quill._
import io.getquill._
import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext

// @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
object PostgresDoobieContextSpec extends Specification {

  implicit def contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  lazy val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  val dc = new PostgresDoobieContext(Literal)
  import dc._

  case class Country(code: String, name: String, population: Int)

  def lookup(code: String) =
    quote { query[Country].filter(_.code == lift(code)) }

  "executeQuery" should {
    "correctly select a country" in {
      val actual   = dc.run(lookup("AND")).transact(xa).unsafeRunSync
      val expected = List(Country("AND", "Andorra", 78000))
      actual should_== expected
    }
  }

}