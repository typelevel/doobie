package doobie.example

import java.io.File

import scalaz.concurrent.Task
import scalaz.stream._
import scalaz.syntax.monad._
import scalaz.{ Monad, Catchable }

import doobie.imports._

import org.http4s._
import org.http4s.dsl._
import org.http4s.server._
import org.http4s.server.blaze.BlazeServer

import _root_.argonaut._, Argonaut._

object Http4sExample extends App {

  // DAO parameterized with the transactor
  val dao = DAO(DriverManagerTransactor[Task]("org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", ""))

  // A wee service
  val service: HttpService = {
    case GET -> Root / lang => 
      Ok(dao.speakerQuery(lang).map(_.asJson.nospaces + "\n")) 
  }

  // Our program
  val tmain: Task[Unit] = 
    for {
      a <- dao.loadDatabase(new File("world.sql"))
      _ <- Task.delay(BlazeServer.newBuilder.mountService(service, "/speakers").run) // port 8080
    } yield ()

  // End of the world
  tmain.run

}

// Data access module parameterized on our transactor, because why not?
case class DAO[M[_]: Monad: Catchable: Capture](xa: Transactor[M]) {

  // A data type we will read from the database
  case class Country(name: String, indepYear: Option[Int])
  object Country {
    implicit val countryCodec: CodecJson[Country] = 
      casecodec2(Country.apply, Country.unapply)("name", "indepYear")
  }

  // Construct an action to load up a database from the specified file.
  def loadDatabase(f: File): M[Unit] =
    sql"RUNSCRIPT FROM ${f.getName} CHARSET 'UTF-8'".update.run.transact(xa).void

  // Construct an action to stream countries where more than 10% of the population speaks `lang`.
  def speakerQuery(lang: String): Process[M, (Country, Int)] =
    xa.transact(sql"""
      SELECT C.NAME, C.INDEPYEAR, PERCENTAGE 
      FROM COUNTRYLANGUAGE CL
      JOIN COUNTRY C ON CL.COUNTRYCODE = C.CODE
      WHERE LANGUAGE = $lang 
      AND PERCENTAGE > 10
      ORDER BY PERCENTAGE DESC
    """.query[(Country, Int)].process)

}
