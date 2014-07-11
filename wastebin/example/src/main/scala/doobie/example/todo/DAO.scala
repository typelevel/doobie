package doobie.example.todo

import scalaz._
import scalaz.effect._
import Scalaz._

import doobie._
import doobie.hi._
import doobie.hi.resultset._

import Model._

// These methods map 1:1 with SQL statements
object DAO {

  def topics(effect: Topic => IO[Unit]): Connection[Unit] =
    connection.push("topics(<effect>)") {
      sql"""
        SELECT topic_id, name FROM topic
        ORDER BY name ASC
      """.executeQuery(sink[Topic](effect))
    }

  def insertTopic(name: String): Connection[Int] =
    connection.push(s"insertTopic($name)") {
      sql"""
        INSERT INTO topic (name)
        VALUES ($name)
      """.executeUpdate
    }

  def selectTopic(id: Int): Connection[Topic] =
    connection.push(s"selectTopic($id)") {
      sql"""
        SELECT topic_id, name
        FROM topic
        WHERE topic_id = $id
      """.executeQuery(getUnique[Topic])
    }

  def selectTopicByName(name: String): Connection[Option[Topic]] =
    connection.push(s"findTopicByName($name)") {
      sql"""
        SELECT topic_id, name
        FROM topic
        WHERE name = $name
      """.executeQuery(getNext[Topic])
    }

  def lastIdentity: Connection[Int] =
    connection.push("lastIdentity") {
      sql"""
        SELECT IDENTITY()
      """.executeQuery(getUnique[Int])
    }

  def init: Connection[Boolean] = 
    connection.push("init") {
      sql"""
        CREATE TABLE topic (
          topic_id IDENTITY,
          name     VARCHAR NOT NULL UNIQUE
        );
      """.execute
    }

}