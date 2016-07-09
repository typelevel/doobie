package doobie.example

import java.io.File

import scalaz.concurrent.Task
import scalaz.stream.Process
import scalaz.syntax.monad._

import doobie.imports._

// JDBC program using the high-level API
object HiUsage {

  // A very simple data type we will read
  case class CountryCode(code: Option[String])
  
  // Program entry point
  def main(args: Array[String]): Unit = {
    val db = DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")
    example.transact(db).unsafePerformSync
  }

  // An example action. Streams results to stdout
  lazy val example: ConnectionIO[Unit] =
    speakerQuery("English", 10).sink(c => FC.delay(println("~> " + c)))

  // Construct an action to find countries where more than `pct` of the population speaks `lang`.
  // The result is a scalaz.stream.Process that can be further manipulated by the caller.
  def speakerQuery(lang: String, pct: Double): Process[ConnectionIO,CountryCode] =
    sql"SELECT COUNTRYCODE FROM COUNTRYLANGUAGE WHERE LANGUAGE = $lang AND PERCENTAGE > $pct".query[CountryCode].process

}

