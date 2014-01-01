package doobie
package dbc

import scalaz.effect.IO
import java.sql

final class Database private (url: String, user: String, pass: String) {

  def run[A](k: Connection[A]): IO[A] =
    for {
      c <- IO(sql.DriverManager.getConnection(url, user, pass))
      a <- connection.run(k, c).ensuring(IO(c.close))
    } yield a

}

object Database {

  def apply[A](url: String, user: String, pass: String)(implicit A: Manifest[A]): IO[Database] =
    IO(Class.forName(A.runtimeClass.getName)).map(_ => new Database(url, user, pass))

}
