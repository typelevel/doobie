package doobie.example

import java.io.File

import scalaz.concurrent.Task
import scalaz.stream._
import scalaz.syntax.monad._

import doobie.hi._
import doobie.hi.connection.ProcessConnectionIOOps
import doobie.std.task._
import doobie.std.string._
import doobie.std.int._
import doobie.std.double._
import doobie.syntax.process._
import doobie.syntax.string._
import doobie.util.transactor._

// Experimenting with transactors and fully-lifted streams
object Hi2 extends App {

  // How about a DAO parameterized on the transactor?
  val dao = DAO(DriverManagerTransactor[Task]("org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", ""))

  // Our program
  lazy val tmain: Task[Unit] = 
    for {
      a <- dao.loadDatabase(new File("world.sql"))
      _ <- dao.speakerQuery("English", 10).map(_.toString).to(io.stdOutLines).runLog
    } yield ()

  // End of the world
  tmain.run

}

// Data access module parameterized on our transactor, because why not?
case class DAO[M[_]](xa: Transactor[M]) {

  // A data type we will read from the database
  case class Country(name: String, indepYear: Option[Int])

  // Construct an action to load up a database from the specified file.
  def loadDatabase(f: File): M[Unit] =
    xa.transact(sql"RUNSCRIPT FROM ${f.getName} CHARSET 'UTF-8'".executeUpdate.void)

  // Construct an action to stream countries where more than `pct` of the population speaks `lang`.
  def speakerQuery(lang: String, pct: Double): Process[M, Country] =
    xa.transact(sql"""
      SELECT C.NAME, C.INDEPYEAR FROM COUNTRYLANGUAGE CL
      JOIN COUNTRY C ON CL.COUNTRYCODE = C.CODE
      WHERE LANGUAGE = $lang 
      AND PERCENTAGE > $pct
    """.process[Country])

}

