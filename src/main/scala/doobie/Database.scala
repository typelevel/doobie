package doobie

import world.ConnectionWorld._
import scalaz.effect.IO
import java.sql._
import scalaz.{ State => _, _}
import Scalaz._
import world.Log

trait Database {
  def run[A](a: Action[A]): IO[(Log, Throwable \/ A)]
}

object Database {

  def apply(clazz: String, url: String): Database =
    new Database {
      def run[A](a: Action[A]): IO[(Log, Throwable \/ A)] =
        IO {
          Class.forName(clazz)
          val conn = DriverManager.getConnection(url, "sa", "") 
          try runc(conn, a) finally conn.close()
        }
    }

}

