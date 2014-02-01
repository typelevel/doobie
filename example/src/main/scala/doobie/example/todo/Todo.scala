package doobie.example.todo

import doobie._
import doobie.hi._

import scalaz._, Scalaz._
import scalaz.effect._, stateTEffect._, IO._

object Todo extends SafeApp {

  val database: IO[Database] =
    Database[org.h2.Driver]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")

  ////// ENTRY POINT

  override def runc: IO[Unit] =
    greet >> (database >>= Repl.run) >> ungreet

  def greet: IO[Unit] =
    putStrLn(s"""
      |Welcome to TODO
      |Type help for instructions.""".stripMargin)

  def ungreet: IO[Unit] =
    putStrLn(s"Bye.\n")

}
