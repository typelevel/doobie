package doobie.util

import doobie.hi.drivermanager.{ getConnection, delay }
import doobie.free.connection.setAutoCommit
import doobie.hi._
import doobie.hi.connection.{ commit, rollback }
import doobie.syntax.catchable._

import scalaz.syntax.monad._
import scalaz.stream.Process

object database {
  
  case class Database(driver: String, url: String, user: String, pass: String) {

    def transact[A](a: ConnectionIO[A]): DriverManagerIO[A] =
      delay(Class.forName(driver)) >> getConnection(url, user, pass) {
        for {
          _ <- setAutoCommit(false)
          a <- a.onException(rollback)
          _ <- commit
        } yield a
      }

  }

}