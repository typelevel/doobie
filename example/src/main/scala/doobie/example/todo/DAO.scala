package doobie.example.todo

import scalaz._
import scalaz.effect._
import Scalaz._

import doobie._
import doobie.hi._

import Model._

object DAO {

  val fixture: List[Topic] = 
    List(Topic(1, "foo"), Topic(2, "bar"), Topic(3, "baz"))

  def topics(sink: Topic => IO[Unit]): Connection[Unit] =
    fixture.traverse_(sink).liftIO[Connection]

}