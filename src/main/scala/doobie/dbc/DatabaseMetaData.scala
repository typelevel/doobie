package doobie
package dbc

import scalaz.effect.IO
import java.sql

trait DatabaseMetaDataFunctions extends DWorld[sql.DatabaseMetaData] {

  type DatabaseMetaData[+A] = Action[A]

  // TODO: ops

}