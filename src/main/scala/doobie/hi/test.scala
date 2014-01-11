package doobie
package hi

import dbc._

import scalaz._
import Scalaz._
import scalaz.effect._
import scalaz.effect.IO._
import java.io.File

// JDBC program using the high-level API
object Test extends SafeApp {
  
  case class Country(code: String, name: String, population: Int) {
    if (code == "NCL") sys.error("Bogus country: NCL")
  }

  object Country {
    implicit def show: Show[Country] = Show.showA
  }

  override def runc: IO[Unit] =
    for {
      l <- util.TreeLogger.newLogger(LogElement("hi.examples"))
      d <- Database[org.h2.Driver]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
      a <- d.run(examples, l).except(IO(_))
      _ <- putStrLn("The answer was: " + a)
      _ <- l.dump
    } yield ()

  def examples: Connection[Int] =
    for {
      _ <- loadDatabase(new File("world.sql"))
      l <- countriesWithSpeakers("French", 30)
      _ <- l.traverseU(putLn(_)).liftIO[Connection]
    } yield l.length

  def loadDatabase(f: File): Connection[Boolean] =
    connection.push(
      s"populate database from ${f.getAbsolutePath}",
      sql"RUNSCRIPT FROM ${f.getName} CHARSET 'UTF-8'".execute)

  def countriesWithSpeakers(s: String, p: Double): Connection[List[Country]] =
    connection.push(
      s"find countries where more than $p% of population speak $s",
      sql"""
        SELECT C.CODE, C.NAME, C.POPULATION
        FROM COUNTRYLANGUAGE L
        JOIN COUNTRY C 
        ON L.COUNTRYCODE = C.CODE
        WHERE LANGUAGE = $s AND PERCENTAGE > $p
        ORDER BY COUNTRYCODE
        """.executeQuery(list[Country]))

}

