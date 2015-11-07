package doobie.example

import scalaz.concurrent.Task

import doobie.imports._
import doobie.contrib.postgresql.pgtypes._

import org.postgresql.geometric._

// Some queries to test using the AnalysisTestSpec in src/test
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

  def arrayTest = 
    sql"""
      SELECT ARRAY[1, 2, NULL] test
    """.query[Option[List[String]]]

  def arrayTest2 = 
    sql"""
      SELECT ARRAY[1, 2, NULL] test
    """.query[String]

  def pointTest =
    sql"""
      SELECT '(1, 2)'::point test
    """.query[PGpoint]

  def pointTest2 = {
    Meta[PostgresPoint.Point] // why not? ... irritating that it must be instantiated. what to do?
    sql"""
      SELECT '(1, 2)'::point test
    """.query[PGcircle]
  }

  def update(code: String, name: Int) = 
    sql"""
      UPDATE COUNTRY SET NAME = $name WHERE CODE = $code
    """.update

  def update2 = 
    sql"""
      UPDATE COUNTRY SET NAME = 'foo' WHERE CODE = 'bkah'
    """.update

  val xa: Transactor[Task] = 
    DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")

}

