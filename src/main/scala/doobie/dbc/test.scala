package doobie
package dbc

import scalaz._
import Scalaz._
import scalaz.effect._
import scalaz.effect.IO._

object Test extends SafeApp {

  import java.io.File
  import connection._
  import resultset._
  import preparedstatement._
  
  case class CountryCode(code: String)

  override def runc: IO[Unit] =
    for {
      d <- Database[org.h2.Driver]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
      a <- d.run(action)
      _ <- putStrLn(a.toString)
    } yield ()

  def action: Connection[String] =
    for {
      _ <- putStrLn("Loading database...").liftIO[Connection]
      _ <- loadDatabase(new File("world.sql"))
      s <- speakerQuery("French", 30)
      _ <- s.traverseU(a => putStrLn(a.toString)).liftIO[Connection]
    } yield "Ok"

  def loadDatabase(f: File): Connection[Unit] =
    prepareStatement("RUNSCRIPT FROM ? CHARSET 'UTF-8'") {
      for {
        _ <- setString(1, f.getName)
        _ <- execute
        _ <- preparedstatement.getConnection(putStrLn("a nested action!").liftIO[Connection])
      } yield()
    }

  // Find countries that speak a given language
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
      next >>= {
        case false => as.point[ResultSet]
        case true  => a >>= { a => unroll0(a :: as) }
      }
    unroll0(Nil).map(_.reverse)
  }

}

