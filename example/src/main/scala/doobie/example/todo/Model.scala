package doobie.example.todo


import doobie._
import doobie.hi._
import java.sql.SQLException

import scalaz._, Scalaz._
import scalaz.effect._, stateTEffect._, IO._
import scalaz.syntax.effect.monadCatchIO._

import dbc.sqlstate.{ postgresql => sqlstate }

import DAO._

object Model {
  
  final case class Topic(id: Int, name: String)

  object Topic {

    def create(name: String): Connection[Option[Topic]] = {
      val a: Connection[Topic] = for { 
        _ <- insertTopic(name) 
        i <- lastIdentity 
        t <- selectTopic(i)
        _ <- connection.commit
      } yield t
      a.catchSqlStateOption(sqlstate.class23.UNIQUE_VIOLATION)
    }

  }

}

