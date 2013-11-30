package doobie

import java.sql._
import doobie.std._
import scalaz.effect._
import scalaz.effect.IO._
import scalaz._
import Scalaz._
import scala.language.implicitConversions
import java.io.File

import doobie.world.{ database => db, connection => conn, statement => st, resultset => rs }
  import conn._

trait ExperimentalSytax {

  // a ddl takes nothing and returns nothing
  def ddl(sql: String): conn.Action[Unit] =
    st.execute.lift(sql)

  // an update takes args and returns an int
  def update[A: Composite](sql: String): A => conn.Action[Int] =
    a => (Composite[A].set(a) >> st.executeUpdate).lift(sql)


  implicit class strSyntax(s: String) {
    def asQuery[I: Composite, O: Composite]: Query[I,O] =
      Query(s)
  }

  def query[I: Composite, O: Composite](sql: String): I => Action[List[O]] =
    ???

  def query0[O: Composite](sql: String): Action[List[O]]  = 
    rs.list[O].lift.lift(sql)


}

object Test2 extends SafeApp with ExperimentalSytax with std.default {

  // An in-memory database
  val ci = db.ConnectInfo[org.h2.Driver]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")

  // Some model types
  case class Id[A](toInt: Int)
  case class CountryCode(code: String)
  case class City(id: Id[City], name: String, code: CountryCode, district: String, population: Int)

  // All cities larger than given population
  val citiesLargerThan: Query[Int, City] =
    """SELECT id, name, countrycode, district, population 
       FROM city 
       WHERE population > ?
       ORDER BY population DESC
    """.asQuery

  // Find countries that speak a given language
  val speakerQuery: Query[(String, Int), CountryCode] =
    """SELECT COUNTRYCODE 
       FROM COUNTRYLANGUAGE 
       WHERE LANGUAGE = ? AND PERCENTAGE > ?
    """.asQuery

  def action: Action[String] =
    for {
      _ <- success(println("Loading database..."))
      _ <- ddl("RUNSCRIPT FROM 'world.sql'")
      s <- speakerQuery(("French", 30))
      _ <- s.traverse(x => success(println(x)))
      c <- citiesLargerThan(9000000)
      _ <- c.traverse(x => success(println(x)))
    } yield "woo!"

  override def runc: IO[Unit] =
    for {
      p <- action.lift.lift(ci)
      // _ <- p._1.traverse(putStrLn)
      _ <- putStrLn(p._2.toString)
    } yield ()

}

