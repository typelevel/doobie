package doobie.example

import scalaz.concurrent.Task

import doobie.syntax.string._
import doobie.util.transactor._
import doobie.util.query.Query0

object AnalysisTest {

  case class Country(name: String, indepYear: Int)

  def speakerQuery(lang: String, pct: Double): Query0[Country] =
    sql"""
      SELECT C.NAME, C.INDEPYEAR, C.CODE FROM COUNTRYLANGUAGE CL
      JOIN COUNTRY C ON CL.COUNTRYCODE = C.CODE
      WHERE LANGUAGE = $lang AND PERCENTAGE > $pct -- comment here ... ${123}
    """.query[Country]

  val xa: Transactor[Task] = 
    DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "rnorris", "")

  val tmain: Task[Unit] = 
    for {
      a <- xa.transact(speakerQuery("ignored", 0).analysis)
      _ <- Task.delay(println(a.summary))
    } yield ()

  def main(args: Array[String]): Unit =
    tmain.run

}

