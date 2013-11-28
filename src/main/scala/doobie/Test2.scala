package doobie

import java.sql._
import doobie.std._
import scalaz.effect._
import scalaz.effect.IO._
import scalaz._
import Scalaz._

import doobie.world.{ database => db, connection => conn, statement => st }

trait ExperimentalSytax {

  // a ddl takes nothing and returns nothing
  def ddl(sql: String): conn.Action[Unit] =
    st.execute.lift(sql)

  // an update takes args and returns an int
  def update[A: Composite](sql: String): A => conn.Action[Int] =
    a => (Composite[A].set(a) >> st.executeUpdate).lift(sql)

}

object Test2 extends SafeApp with ExperimentalSytax with std.default {

  val ci = db.ConnectInfo[org.h2.Driver]("jdbc:h2:/tmp/foo/bar", "sa", "")

  val count: Statement[(Int, String, Option[String]), Int] =
    Statement("SELECT COUNT(*) FROM TEST WHERE ID = ? AND NAME = ? AND FOO = ?")

  val bad: Statement[Int, Int] =
    Statement("BOGUS")

  val defns = 
      ddl("CREATE TABLE IF NOT EXISTS TEST(ID INT, NAME VARCHAR NOT NULL, FOO VARCHAR)") >>
      ddl("CREATE TABLE IF NOT EXISTS ADA(ID INT, NAME VARCHAR NOT NULL)")

  val insert = {
    // Boolean mapping, just for this definition
    implicit val booleanMapping = boolean.as[String]("Y", "N")
    update[(Int, String, Boolean)]("INSERT INTO TEST VALUES (?, ?, ?)")
  }

  def action: conn.Action[String] =
    for {
      _ <- defns
      _ <- conn.commit
      n <- insert((1, "Steve", true)) // todo: this should take a sequence of args
      _ <- conn.rollback // forget that insert
      _ <- count((1, "hi", None))
      // _ <- bad(42)
      _ <- conn.commit
    } yield "woo! " + n

  override def runc: IO[Unit] =
    for {
      p <- action.lift.lift(ci)
      _ <- p._1.traverse(putStrLn)
      _ <- putStrLn(p._2.toString)
    } yield ()

}

