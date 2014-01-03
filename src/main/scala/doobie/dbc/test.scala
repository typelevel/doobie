package doobie
package dbc

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
      for { // this is a ResultSet[Unit]
        _ <- setString(1, f.getName)
        _ <- execute
        _ <- getConnection(putStrLn("a nested action!").liftIO[Connection])
      } yield()
    }

  def speakerQuery(s: String, p: Int): Connection[List[CountryCode]] =
    prepareStatement("SELECT COUNTRYCODE FROM COUNTRYLANGUAGE WHERE LANGUAGE = ? AND PERCENTAGE > ?") {
      for {
        _ <- setString(1, s)
        _ <- setInt(2, p)
        l <- executeQuery(unroll(getString(1).map(CountryCode(_))))
      } yield l
    }

  def unroll[A](a: ResultSet[A]): ResultSet[List[A]] = {
    def unroll0(as: List[A]): ResultSet[List[A]] =
      IO(println("Are we trampolining? Stack depth is " + (new Exception).getStackTrace.length)).liftIO[ResultSet] >>
      next >>= {
        case false => as.point[ResultSet]
        case true  => a >>= { a => unroll0(a :: as) }
      }
    unroll0(Nil).map(_.reverse)
  }

}

