package doobie.example

import doobie._
import doobie.hi._

import scalaz.effect.{IO, SafeApp}
import scalaz.effect.IO.putStrLn
import scalaz.stream.Process
import java.io.File

// JDBC program using the high-level API
object HiUsage extends SafeApp {

  // A simple model with nested case classes
  case class Code(code: String) // { require(code != "NCL", "arbitrary failure") }
  case class Country(code: Code, name: String, population: Int)

  // Connection information
  val ta: Transactor = 
    DriverManagerTransactor[org.h2.Driver]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")

  // Entry point
  override def runc: IO[Unit] =
    ta.exec(examples).except(t => putStrLn("failed: " + t))

  // Some examples to run on the same connection
  def examples: DBIO[Unit] =
    for {
      _ <- loadDatabase(new File("world.sql"))
      _ <- speakersOf("French").take(10).map(_.name).sink(putStrLn) // stream top 10 names to stdout
    } yield ()

  // Load the database from a file
  def loadDatabase(f: File): DBIO[Boolean] =
    sql"RUNSCRIPT FROM ${f.getName} CHARSET 'UTF-8'".execute

  // Construct a stream transformer for countries that speak the given language
  def speakersOf(s: String): Process[DBIO, Country] =
    sql"""
      SELECT C.CODE, C.NAME, C.POPULATION
      FROM COUNTRYLANGUAGE L
      JOIN COUNTRY C 
      ON L.COUNTRYCODE = C.CODE
      WHERE LANGUAGE = $s
      ORDER BY PERCENTAGE DESC
    """.process[Country]

}
