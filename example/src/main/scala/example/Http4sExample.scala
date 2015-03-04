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

// Example streaming results straight to the HTTP response
// $ curl http://localhost:8080/speakers/French
object Http4sExample {

  // A data type we will read from the database, with an Argonaut serializer
  case class Country(name: String, indepYear: Option[Int])
  object Country {
    implicit val countryCodec: CodecJson[Country] = 
      casecodec2(Country.apply, Country.unapply)("name", "indepYear")
  }

  // A wee service
  def service(xa: Transactor[Task]): HttpService = {
    case GET -> Root / lang => 
      Ok(speakerQuery(lang).transact(xa).map(_.asJson.nospaces + "\n")) 
  }

  // Entry point
  def main(args: Array[String]): Unit = {
    val xa = DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")
    BlazeServer.newBuilder.mountService(service(xa), "/speakers").run
  }

  // Construct an action to stream countries where more than 10% of the population speaks `lang`.
  def speakerQuery(lang: String): Process[ConnectionIO, (Country, Int)] =
    sql"""
      SELECT C.NAME, C.INDEPYEAR, PERCENTAGE 
      FROM COUNTRYLANGUAGE CL
      JOIN COUNTRY C ON CL.COUNTRYCODE = C.CODE
      WHERE LANGUAGE = $lang 
      AND PERCENTAGE > 10
      ORDER BY PERCENTAGE DESC
    """.query[(Country, Int)].process

}
