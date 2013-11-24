package doobie

import world.ConnectionWorld._
import scalaz.effect.IO
import java.sql._
import scalaz.{ State => _, _}
import Scalaz._

trait Database {
  def run[A](a: Action[A]): IO[(State, Throwable \/ A)]
}

object Database {

  def apply(clazz: String, url: String): Database =
    new Database {
      def run[A](a: Action[A]): IO[(State, Throwable \/ A)] =
        IO {
          Class.forName(clazz)
          val conn = DriverManager.getConnection(url, "sa", "") 
          try a.unsafeRun(conn) finally conn.close()
        }
    }

}

