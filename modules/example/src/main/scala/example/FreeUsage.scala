// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.example

import cats.effect.IO
import cats.implicits._
import doobie._, doobie.implicits._
import java.io.File

// JDBC program using the low-level API
object FreeUsage {

  case class CountryCode(code: String)

  def main(args: Array[String]): Unit = {
    val db = Transactor.fromDriverManager[IO]("org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")
    db.trans.apply(examples.void).unsafeRunSync
  }

  def examples: ConnectionIO[String] =
    for {
      _ <- FC.delay(println("Loading database..."))
      _ <- loadDatabase(new File("example/world.sql"))
      s <- speakerQuery("English", 10)
      _ <- s.traverse(a => FC.delay(println(a)))
    } yield "Ok"

  def loadDatabase(f: File): ConnectionIO[Unit] =
    for {
      ps <- FC.prepareStatement("RUNSCRIPT FROM ? CHARSET 'UTF-8'")
      _  <- FC.embed(ps, (FPS.setString(1, f.getName) >> FPS.execute) guarantee FPS.close)
    } yield ()

  def speakerQuery(s: String, p: Double): ConnectionIO[List[CountryCode]] =
    for {
      ps <- FC.prepareStatement("SELECT COUNTRYCODE FROM COUNTRYLANGUAGE WHERE LANGUAGE = ? AND PERCENTAGE > ?")
      l  <- FC.embed(ps, speakerPS(s, p) guarantee FPS.close)
    } yield l

  def speakerPS(s: String, p: Double): PreparedStatementIO[List[CountryCode]] =
    for {
      _  <- FPS.setString(1, s)
      _  <- FPS.setDouble(2, p)
      rs <- FPS.executeQuery
      l  <- FPS.embed(rs, unroll(FRS.getString(1).map(CountryCode(_))) guarantee FRS.close)
    } yield l

  def unroll[A](a: ResultSetIO[A]): ResultSetIO[List[A]] = {
    def unroll0(as: List[A]): ResultSetIO[List[A]] =
      FRS.next >>= {
        case false => as.pure[ResultSetIO]
        case true  => a >>= { a => unroll0(a :: as) }
      }
    unroll0(Nil).map(_.reverse)
  }

}
