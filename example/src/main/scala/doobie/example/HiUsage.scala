package doobie.example


import doobie._
import doobie.hi._

import scalaz._
import Scalaz._
import scalaz.effect._
import scalaz.effect.IO._
import scalaz.stream.Process
import java.io.File

// JDBC program using the high-level API
object HiUsage extends SafeApp {

  // A simple model object with a Show instance  
  case class Country(code: String, name: String, population: Int) {
    require(code != "CHE", "example random failure")
  }
  object Country {
    implicit def show: Show[Country] = 
      Show.showA
  }

  // treat File as a primitive column type (contrived)
  implicit def primFile: Prim[File] =
    Prim[String].xmap(new File(_), _.getName)

  // Transactor, which handles the log and connections
  val ta = DriverManagerTransactor[org.h2.Driver]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")

  // Entry point
  override def runc: IO[Unit] =
    examples.run(ta).except(t => putStrLn("failed: " + t))

  // Some examples to run together in a single transaction
  def examples: DBIO[Unit] =
    for {
      _ <- loadDatabase(new File("world.sql"))
      _ <- countriesWithSpeakers("French").take(10).sink(putLn)
    } yield ()

  def loadDatabase(f: File): DBIO[Boolean] =
    sql"""
      RUNSCRIPT FROM $f CHARSET 'UTF-8'
    """.execute

  def countriesWithSpeakers(s: String): Process[DBIO, Country] =
    sql"""
      SELECT C.CODE, C.NAME, C.POPULATION
      FROM COUNTRYLANGUAGE L
      JOIN COUNTRY C 
      ON L.COUNTRYCODE = C.CODE
      WHERE LANGUAGE = $s
      ORDER BY PERCENTAGE DESC
      """.process[Country]

}
