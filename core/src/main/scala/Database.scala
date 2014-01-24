package doobie

import dbc._
import scalaz.Show
import scalaz.effect.IO
import scalaz.effect.kleisliEffect._
import scalaz.syntax.effect.monadCatchIO._
import java.sql

// review api
final class Database private (url: String, user: String, pass: String) {

  def run[A: Show](k: Connection[A], l: Log[LogElement]): IO[A] =
    for {
      c <- l.log(LogElement(s"getConnection($url, $user, ***)"), IO(sql.DriverManager.getConnection(url, user, pass)))
      a <- l.log(LogElement("gosub/cleanup"), (k ensuring connection.close).run((l, c)))
    } yield a

}

object Database {

  def apply[A](url: String, user: String, pass: String)(implicit A: Manifest[A]): IO[Database] =
    IO(Class.forName(A.runtimeClass.getName)).map(_ => new Database(url, user, pass))

}
