package doobie
package dbc

import scalaz.effect.IO
import java.sql

object databasemetadata extends util.TWorld[sql.DatabaseMetaData] {

  type DatabaseMetaData[+A] = Action[A]

  private[dbc] def run[A](a: DatabaseMetaData[A], s: sql.DatabaseMetaData): IO[A] = 
    eval(a, s).map(_._2)
  
}