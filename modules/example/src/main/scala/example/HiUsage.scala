// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package example

import cats.Show
import cats.effect.{ IO, IOApp, ExitCode }
import cats.implicits._
import fs2.Stream
import doobie._
import doobie.implicits._

// JDBC program using the high-level API
object HiUsage extends IOApp {

  // A very simple data type we will read
  final case class CountryCode(code: Option[String])
  object CountryCode {
    implicit val show: Show[CountryCode] = Show.fromToString
  }

  // Program entry point
  def run(args: List[String]): IO[ExitCode] = {
    val db = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
    )
    example.transact(db).as(ExitCode.Success)
  }

  // An example action. Streams results to stdout
  lazy val example: ConnectionIO[Unit] =
    speakerQuery("English", 10).evalMap(c => FC.delay(println(show"~> $c"))).compile.drain

  // Construct an action to find countries where more than `pct` of the population speaks `lang`.
  // The result is a fs2.Stream that can be further manipulated by the caller.
  def speakerQuery(lang: String, pct: Double): Stream[ConnectionIO,CountryCode] =
    sql"SELECT COUNTRYCODE FROM COUNTRYLANGUAGE WHERE LANGUAGE = $lang AND PERCENTAGE > $pct".query[CountryCode].stream

}
