package doobie
package dbc

import scalaz.effect.IO
import java.sql

object databasemetadata extends DWorld[sql.DatabaseMetaData] {

  type DatabaseMetaData[+A] = Action[A]

}