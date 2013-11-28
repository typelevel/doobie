package doobie

import java.sql._
import doobie.std.default._
import scalaz.effect._
import scalaz.effect.IO._
import scalaz._
import Scalaz._

import doobie.world.{ database => db, connection => conn, statement => st }

object Test2 extends SafeApp {

  val ci = db.ConnectInfo[org.h2.Driver]("jdbc:h2:/tmp/foo/bar", "sa", "")

  val count: Statement[(Int, String, Option[String]), Int] =
    Statement("SELECT COUNT(*) FROM TEST WHERE ID = ? AND NAME = ? AND FOO = ?")

  val bad: Statement[Int, Int] =
    Statement("BOGUS")

  val create =
    st.execute.lift("CREATE TABLE IF NOT EXISTS TEST(ID INT, NAME VARCHAR NOT NULL, FOO VARCHAR)")

  val insert =
    st.execute.lift("INSERT INTO TEST VALUES (1, 'Steve', 'bar')")

  def action: conn.Action[String] =
    for {
      _ <- create
      _ <- insert
      _ <- count((1, "hi", None))
      // _ <- bad(42)
      _ <- conn.commit
    } yield "woo!"

  def session: db.Action[String] =
    for {
      a <- action.lift
      b <- action.lift
    } yield a + b

  override def runc: IO[Unit] =
    for {
      _ <- putStrLn("hi")
      p <- session.lift(ci)
      _ <- p._1.traverse(putStrLn)
      _ <- putStrLn(p._2.toString)
    } yield ()

}

// // Create a table
// val create = "CREATE TABLE IF NOT EXISTS TEST(ID INT, NAME VARCHAR NOT NULL, FOO VARCHAR)"
// conn.prepareStatement(create).execute()
