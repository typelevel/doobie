package doobie.example

import scalaz.concurrent.Task

import doobie.syntax.string._
import doobie.util.transactor._
import doobie.util.query.Query0
import doobie.util.update.Update0

object AnalysisTest {

  case class Country(name: String, indepYear: Int)

  def speakerQuery(lang: String, pct: Double) =
    sql"""
      SELECT C.NAME, C.INDEPYEAR, C.CODE FROM COUNTRYLANGUAGE CL
      JOIN COUNTRY C ON CL.COUNTRYCODE = C.CODE
      WHERE LANGUAGE = $lang AND PERCENTAGE > $pct
    """.query[Country]

  def speakerQuery2 =
    sql"""
      SELECT C.NAME, C.INDEPYEAR, C.CODE FROM COUNTRYLANGUAGE CL
      JOIN COUNTRY C ON CL.COUNTRYCODE = C.CODE
    """.query[(String, Option[Short], String)]

  def update(code: String, name: Int) = 
    sql"""
      UPDATE COUNTRY SET NAME = $name WHERE CODE = $code
    """.update

  def update2 = 
    sql"""
      UPDATE COUNTRY SET NAME = 'foo' WHERE CODE = 'bkah'
    """.update

  val xa: Transactor[Task] = 
    DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "rnorris", "")

}

