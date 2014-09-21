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

object HiUsage2 extends App {

  val xa = DriverManagerTransactor[Task]("org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
  val dao = HiUsage2DAO(xa)

  // Our program
  lazy val tmain: Task[Unit] = 
    for {
      a <- dao.loadDatabase(new File("world.sql"))
      x <- xa.transact(HiUsage2DAO.speakerQuery("ignored", 0).check)
      _ <- Task.delay(Console.println(x))
      // _ <- dao.speakerQuery("English", 10).map(_.toString).to(io.stdOutLines).runLog
    } yield ()

  // End of the world
  tmain.run

}

// Data access module parameterized on our transactor, because why not?
case class HiUsage2DAO[M[_]](xa: Transactor[M]) {

  // Construct an action to load up a database from the specified file.
  def loadDatabase(f: File): M[Unit] =
    xa.transact(HiUsage2DAO.loadDatabase(f).run.void)

  // Construct an action to stream countries where more than `pct` of the population speaks `lang`.
  def speakerQuery(lang: String, pct: Double): Process[M, HiUsage2DAO.Country] =
    xa.transact(HiUsage2DAO.speakerQuery(lang, pct).run)

}

object HiUsage2DAO {

  import doobie.util.query.Query0
  import doobie.util.update.Update0
  import doobie.util.invariant.MappingViolation

  // A data type we will read from the database
  case class Country(name: Option[String], indepYear: Int)

  def loadDatabase(f: File): Update0 =
    sql"RUNSCRIPT FROM ${f.getName} CHARSET 'UTF-8'".update

  def speakerQuery(lang: String, pct: Double): Query0[Country] =
    sql"""
      SELECT C.NAME, C.INDEPYEAR, C.CODE FROM COUNTRYLANGUAGE CL
      JOIN COUNTRY C ON CL.COUNTRYCODE = C.CODE
      WHERE LANGUAGE = $lang AND PERCENTAGE > $pct
    """.query[Country]

}