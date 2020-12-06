// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.quill
package issue

import cats.effect._
import doobie._
import doobie.implicits._
import io.getquill._
import scala.concurrent.ExecutionContext

// https://github.com/tpolecat/doobie/issues/1067
class `1067` extends munit.FunSuite {

  implicit def contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  lazy val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  val dc = new DoobieContext.Postgres(Literal)
  import dc._

  case class Country(name: String, indepYear: Option[Short])

  test("Issue1067 - correctly select many countries, with a null in last position") {
    val stmt     = quote { query[Country] }
    val actual   = run(stmt).transact(xa).unsafeRunSync()
    assertEquals(actual.count(_.indepYear.isDefined), 192)
    assertEquals(actual.count(_.indepYear.isEmpty), 47)
  }

}