package doobie

import java.sql._
import doobie.std._
import scalaz.effect._
import scalaz.effect.IO._
import scalaz._
import Scalaz._
import scala.language.implicitConversions

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

  val ci = db.ConnectInfo[org.h2.Driver]("jdbc:h2:/tmp/foo/bar", "sa", "")

  implicit val booleanMapping = boolean.as[String]("Y", "N")

  val count: Statement[Int, Int] = 
    "SELECT COUNT(*) FROM TEST WHERE ID = ?"

  val bad: Statement[Int, Int] =
    "BOGUS"

  val defns = 
      ddl("CREATE TABLE IF NOT EXISTS TEST(ID INT, NAME VARCHAR NOT NULL, FOO VARCHAR)") >>
      ddl("CREATE TABLE IF NOT EXISTS ADA(ID INT, NAME VARCHAR NOT NULL)")

  val insert = {
    // Boolean mapping, just for this definition
    update[(Int, String, Boolean)]("INSERT INTO TEST VALUES (?, ?, ?)")
  }

  val select: Statement[String, (Int, String, Option[Boolean])] =
    "SELECT ID, NAME, FOO FROM TEST WHERE NAME = ?"

  import conn._

  def action: conn.Action[String] =
    for {
      _ <- defns
      _ <- commit
      n <- List((1, "Steve", true), (2, "Bob", false)).traverse(insert).map(_.sum)
      _ <- rollback // forget that insert
      c <- count(1).map(_.head)
      x <- select("Bob")
      _ <- x.traverse(r => conn.success(println(r)))
      // _ <- bad(42)
      _ <- commit
    } yield "woo! " + n + " count was " + c

  override def runc: IO[Unit] =
    for {
      p <- action.lift.lift(ci)
      // _ <- p._1.traverse(putStrLn)
      _ <- putStrLn(p._2.toString)
    } yield ()

}

