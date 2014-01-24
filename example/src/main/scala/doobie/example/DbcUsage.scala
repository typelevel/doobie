package doobie.example


import doobie._
import doobie.dbc._

import scalaz._
import Scalaz._
import scalaz.effect._
import scalaz.effect.IO
import scalaz.effect.IO._
import scalaz.effect.kleisliEffect._
import scalaz.syntax.effect.monadCatchIO._

// JDBC program using the low-level API
object DbcUsage extends SafeApp {

  import java.io.File
  import connection._
  import resultset._
  import preparedstatement._
  
  case class CountryCode(code: String)

  val database: IO[Database] =
    Database[org.h2.Driver]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
  
  override def runc: IO[Unit] =
    for {
      d <- database 
      l <- util.TreeLogger.newLogger(LogElement("dbc.examples"))
      a <- d.run(examples, l).except(t => IO(t.toString))
      _ <- putStrLn(a)
      _ <- l.dump
    } yield ()

  def examples: Connection[String] =
    for {
      _ <- putStrLn("Loading database...").liftIO[Connection] 
      _ <- loadDatabase(new File("example/world.sql"))
      s <- speakerQuery("French", 0)
      _ <- s.traverseU(a => putStrLn(a.toString)).liftIO[Connection]
    } yield "Ok"

  def loadDatabase(f: File): Connection[Unit] =
    prepareStatement("RUNSCRIPT FROM ? CHARSET 'UTF-8'") {
      for { 
        _ <- setString(1, f.getName)
        _ <- execute
        _ <- getConnection(putStrLn("a nested Connection action could go here").liftIO[Connection])
      } yield()
    }

  def speakerQuery(s: String, p: Int): Connection[List[CountryCode]] =
    prepareStatement("SELECT COUNTRYCODE FROM COUNTRYLANGUAGE WHERE LANGUAGE = ? AND PERCENTAGE > ?") {
      for {
        _ <- setString(1, s)
        _ <- setInt(2, p)
        l <- executeQuery(unroll(getString(-1).map(CountryCode(_))))
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

