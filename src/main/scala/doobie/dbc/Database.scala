package doobie
package dbc

import scalaz.Show
import scalaz.effect.IO
import java.sql

final class Database private (url: String, user: String, pass: String) {

  def run[A](k: Connection[A], l: Log[LogElement]): IO[A] =
    for {
      c <- l.log(s"getConnection($url, $user, ***)", IO(sql.DriverManager.getConnection(url, user, pass)))(implicitly, Show.showA)
      a <- l.log("database session", connection.run(k, l, c).ensuring(connection.run(connection.close, l, c)))(implicitly, Show.showA)
    } yield a

}

object Database {

  def apply[A](url: String, user: String, pass: String)(implicit A: Manifest[A]): IO[Database] =
    IO(Class.forName(A.runtimeClass.getName)).map(_ => new Database(url, user, pass))

}
