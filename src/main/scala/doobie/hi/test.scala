package doobie
package hi

import dbc._

import scalaz._
import Scalaz._
import scalaz.effect._
import scalaz.effect.IO._

// JDBC program using the low-level API
object Test extends SafeApp {

  import java.io.File
  import connection._
  import resultset._
  import preparedstatement._
  
  case class CountryCode(code: String)

  val database: IO[Database] =
    Database[org.h2.Driver]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
  
  override def runc: IO[Unit] =
    for {
      d <- database // IO[Database]
      a <- d.run(examples) // Connection[A] => IO[A]
      _ <- putStrLn(a)
    } yield ()

  def examples: Connection[String] =
    for {
      _ <- putStrLn("Loading database...").liftIO[Connection] // All effect worlds have MonadIO
      _ <- loadDatabase(new File("world.sql"))
      s <- speakerQuery("French", 0)
      _ <- s.traverseU(a => putStrLn(a.toString)).liftIO[Connection]
    } yield "Ok"

  def loadDatabase(f: File): Connection[Unit] =
    prepareStatement("RUNSCRIPT FROM ? CHARSET 'UTF-8'") {
      for { 
        _ <- set(1, f.getName)
        _ <- execute
        _ <- getConnection(putStrLn("a nested action!").liftIO[Connection])
      } yield()
    }

  def speakerQuery(s: String, p: Int): Connection[List[CountryCode]] =
    prepareStatement("""
      SELECT COUNTRYCODE 
      FROM COUNTRYLANGUAGE 
      WHERE LANGUAGE = ? AND PERCENTAGE > ? 
      ORDER BY COUNTRYCODE
      """) {
      for {
        _ <- set(1, (s, p))
        l <- executeQuery(list(get[CountryCode](1)))
      } yield l
    }

}

