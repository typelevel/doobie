package doobie

import java.sql._
import doobie.std.default._
import doobie.world.ConnectionWorld._
import scalaz.effect._
import scalaz.effect.IO._
import scalaz._
import Scalaz._

object Test2 extends SafeApp {

  val db = Database("org.h2.Driver", "jdbc:h2:/tmp/foo/bar")

  val count: Statement[(Int, String, Option[String]), Int] =
    Statement("SELECT COUNT(*) FROM TEST WHERE ID = ? AND NAME = ? AND FOO = ?")

  def action: Action[String] =
    for {
      _ <- count((1, "hi", None))
      _ <- commit
    } yield "woo!"

  override def runc: IO[Unit] =
    for {
      _ <- putStrLn("hi")
      a <- db.run(action)
      _ <- a._1.traverse(putStrLn)
      _ <- putStrLn(a._2.toString)
    } yield ()

}

// // Create a table
// val create = "CREATE TABLE IF NOT EXISTS TEST(ID INT, NAME VARCHAR NOT NULL, FOO VARCHAR)"
// conn.prepareStatement(create).execute()
