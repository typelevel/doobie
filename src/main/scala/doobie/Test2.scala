package doobie

import java.sql._
import doobie.std._
import scalaz.effect._
import scalaz.effect.IO._
import scalaz._
import Scalaz._
import scala.language._
import java.io.File

import doobie.world._
import doobie.world.connection.{ Action => DBIO, success => dbio, _ }
import doobie.world.database.ConnectInfo


object Test2 extends SafeApp with ExperimentalSytax with std.default {

  // An in-memory database
  val ci = ConnectInfo[org.h2.Driver]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")

  // Some model types
  case class Id[A](toInt: Int)
  case class CountryCode(code: String)
  case class City(id: Id[City], name: String, code: CountryCode, population: Int)

  // All cities larger than given population
  // TODO: I want to declare this as Int => DBIO[List[City]]
  def citiesLargerThan(i: Int): DBIO[List[City]] =
    """SELECT id, name, countrycode, population 
       FROM city 
       WHERE population > ?
       ORDER BY population DESC
    """.asParameterizeQuery(i).returning[City]

  // Find countries that speak a given language
  def speakerQuery(s: String, p: Int): DBIO[List[CountryCode]] =
    """SELECT COUNTRYCODE 
       FROM COUNTRYLANGUAGE 
       WHERE LANGUAGE = ? AND PERCENTAGE > ?
    """.asParameterizeQuery((s, p)).returning[CountryCode]

  // DBIO[A] describes a computation that interacts with the database and computes
  // a value of type A. 
  def action: DBIO[String] =
    for {
      _ <- dbio(println("Loading database...")) // how about a real MonadIO?
      _ <- "RUNSCRIPT FROM 'world.sql'".asUnitStatement // DB-specific stuff, no problem
      s <- speakerQuery("French", 30)
      _ <- s.traverse(x => dbio(println(x)))
      c <- citiesLargerThan(9000000)
      _ <- c.traverse(x => dbio(println(x)))
    } yield "woo!"

  // Apply connection info to a DBIO[A] to get an IO[(Log, Throwable \/ A)]
  override def runc: IO[Unit] =
    for {
      p <- action.lift(ci)
      _ <- p._1.zipWithIndex.takeRight(10).traverse(x => putStrLn(x.toString)) // last 10 log entries
      _ <- putStrLn(p._2.toString) // the answer
    } yield ()

}

//////////


trait ExperimentalSytax {

  implicit class strSyntax(s: String) {

    def asUnitStatement: DBIO[Unit] =
      statement.execute.lift(s)

    def asQuery[I: Composite, O: Composite]: Query[I,O] =
      Query(s)

    def asParameterizeQuery[I: Composite](i: I) = new {
      def returning[O: Composite] =
        Query[I,O](s).apply(i)
    }

  }

}

