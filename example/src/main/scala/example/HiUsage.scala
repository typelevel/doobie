package doobie.example

import java.io.File

import scalaz.concurrent.Task
import scalaz.stream.Process
import scalaz.syntax.monad._

import doobie.hi._
import doobie.std.task._
import doobie.std.string._
import doobie.std.double._
import doobie.syntax.process._
import doobie.syntax.string._
import doobie.util.database.Database

// JDBC program using the high-level API
object HiUsage {

  // A very simple data type we will read
  case class CountryCode(code: Option[String])

  // Database is just a module of combinators parameterized on connect info
  lazy val db = Database("org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
  
  // Program entry point simply delegates to a scalaz.concurrent.Task; could also be scalaz.effect.IO
  def main(args: Array[String]): Unit =
    tmain.run

  // Our logical entry point is a Task[Unit]. One of the things it does is a database interaction.
  lazy val tmain: Task[Unit] = 
    for {
      a <- db.transact(example).liftK[Task]
      _ <- Task.delay(Console.println(a))
    } yield ()

  // Our example loads up a test database and does a query, streaming results to the console. Note
  // that this is a value; nothing "happens" until it is transformed into an effectful monad and
  // executed by main above.
  lazy val example: ConnectionIO[String] =
    for {
      _ <- connection.delay(println("Loading database..."))
      _ <- loadDatabase(new File("example/world.sql"))
      _ <- speakerQuery("French", 0.7).sink(c => connection.delay(println("~> " + c))) // streaming; constant space
    } yield "Ok"

  // Construct an action to load up a database from the specified file.
  def loadDatabase(f: File): ConnectionIO[Unit] =
    sql"RUNSCRIPT FROM ${f.getName} CHARSET 'UTF-8'".executeUpdate.void

  // Construct an action to find countries where more than `pct` of the population speaks `lang`.
  // The result is a scalaz.stream.Process that can be further manipulated by the caller.
  def speakerQuery(lang: String, pct: Double): Process[ConnectionIO,CountryCode] =
    sql"SELECT COUNTRYCODE FROM COUNTRYLANGUAGE WHERE LANGUAGE = $lang AND PERCENTAGE > $pct".process[CountryCode]

}

