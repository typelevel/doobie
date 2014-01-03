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
  
  case class CountryCode(code: String)

  override def runc: IO[Unit] =
    for {
      d <- Database[org.h2.Driver]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
      a <- d.run(examples) 
      _ <- putStrLn(a)
    } yield ()

  def examples: Connection[String] =
    for {
      _ <- putStrLn("Loading database...").liftIO[Connection] 
      _ <- loadDatabase(new File("world.sql"))
      l <- countriesWithSpeakers("French", 30)
      _ <- l.traverseU(c => putStrLn(c.toString)).liftIO[Connection]
    } yield "Ok"

  def loadDatabase(f: File): Connection[Boolean] =
    sql"RUNSCRIPT FROM ${f.getName} CHARSET 'UTF-8'".execute

  def countriesWithSpeakers(s: String, p: Int): Connection[List[CountryCode]] =
    sql"""
      SELECT COUNTRYCODE 
      FROM COUNTRYLANGUAGE 
      WHERE LANGUAGE = $s AND PERCENTAGE > $p
      ORDER BY COUNTRYCODE
      """.executeQuery(list[CountryCode])

}

