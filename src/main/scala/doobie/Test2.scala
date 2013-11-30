package doobie

import java.sql._
import doobie.std._
import scalaz.effect._
import scalaz.effect.IO._
import scalaz._
import Scalaz._
import scala.language.implicitConversions
import java.io.File

import doobie.world.{ database => db, connection => conn, statement => st }

trait ExperimentalSytax {

  // a ddl takes nothing and returns nothing
  def ddl(sql: String): conn.Action[Unit] =
    st.execute.lift(sql)

  // an update takes args and returns an int
  def update[A: Composite](sql: String): A => conn.Action[Int] =
    a => (Composite[A].set(a) >> st.executeUpdate).lift(sql)

  // string syntax
  implicit def str2stmt[I: Composite, O: Composite](s: String): Statement[I, O] =
    Statement(s) 

}

object Test2 extends SafeApp with ExperimentalSytax with std.default {
  import conn._

  // An in-memory database
  val ci = db.ConnectInfo[org.h2.Driver]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")

  // Count countries that speak a given language
  val speakerQuery: Statement[(String, Int), String] =
    "SELECT COUNTRYCODE FROM COUNTRYLANGUAGE WHERE LANGUAGE = ? AND PERCENTAGE > ?"

  def action: Action[String] =
    for {
      _ <- success(println("Loading database..."))
      _ <- ddl("RUNSCRIPT FROM 'world.sql'")
      s <- speakerQuery(("English", 50))
      _ <- s.traverse(x => success(println(x)))
    } yield "woo!"

  override def runc: IO[Unit] =
    for {
      p <- action.lift.lift(ci)
      // _ <- p._1.traverse(putStrLn)
      _ <- putStrLn(p._2.toString)
    } yield ()

}

