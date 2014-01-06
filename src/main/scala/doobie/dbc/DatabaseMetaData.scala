package doobie
package dbc

import scalaz.effect.IO
import java.sql

object databasemetadata extends DWorld[sql.DatabaseMetaData] {

  type DatabaseMetaData[+A] = Action[A]

  private[dbc] def run[A](a: DatabaseMetaData[A], l: Log[LogElement], s: sql.DatabaseMetaData): IO[A] = 
    eval(a, l, s).map(_._2)
  
}