package doobie.example

import java.io.File

import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.stream.Process

import doobie.hi._
import doobie.hi.{ drivermanager => DM }
import doobie.hi.{ connection => C }
import doobie.hi.{ preparedstatement => PS }
import doobie.hi.{ resultset => RS }

import doobie.std.task._
import doobie.std.string._
import doobie.std.double._

import doobie.syntax.catchable._
import doobie.syntax.process._

// JDBC program using the high-level API
object HiUsage {

  case class CountryCode(code: String)
  
  def main(args: Array[String]): Unit =
    tmain.translate[Task].run

  val tmain: DriverManagerIO[Unit] = 
    for {
      _ <- DM.delay(Class.forName("org.h2.Driver"))
      a <- DM.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")(examples).except(_.toString.point[DriverManagerIO])
      _ <- DM.delay(Console.println(a))
    } yield ()

  def examples: ConnectionIO[String] =
    for {
      _ <- C.delay(println("Loading database..."))
      _ <- loadDatabase(new File("example/world.sql"))
      s <- speakerQuery("French", 0.7).sink(c => C.delay(println("~> " + c)))
    } yield "Ok"

  def loadDatabase(f: File): ConnectionIO[Unit] =
    C.prepareStatement("RUNSCRIPT FROM ? CHARSET 'UTF-8'")(PS.set(f.getName) >> PS.executeUpdate.void)

  def speakerQuery(s: String, p: Double): Process[ConnectionIO,CountryCode] =
    C.process[CountryCode]("SELECT COUNTRYCODE FROM COUNTRYLANGUAGE WHERE LANGUAGE = ? AND PERCENTAGE > ?", PS.set((s, p)))

}
