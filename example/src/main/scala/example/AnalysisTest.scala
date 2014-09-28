package doobie.example

import scalaz.concurrent.Task
import scalaz.syntax.monad._

import doobie.std.task._
import doobie.std.string._
import doobie.std.int._
import doobie.std.float._
import doobie.syntax.string._
import doobie.util.transactor._
import doobie.util.query.Query0

object AnalysisTest {

  case class Country(name: String, indepYear: Option[Int])

  def speakerQuery(lang: String, pct: Float): Query0[Country] =
    sql"""
      SELECT C.NAME, C.INDEPYEAR FROM COUNTRYLANGUAGE CL
      JOIN COUNTRY C ON CL.COUNTRYCODE = C.CODE
      WHERE LANGUAGE = $lang AND PERCENTAGE > $pct
    """.query[Country]

  val xa: Transactor[Task] = 
    DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "rnorris", "")

  val tmain: Task[Unit] = 
    xa.transact(speakerQuery("ignored", 0).analysis) >>= (_.print)

  def main(args: Array[String]): Unit =
    tmain.run

}