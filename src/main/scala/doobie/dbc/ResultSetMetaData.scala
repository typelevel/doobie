package doobie
package dbc

import scalaz.effect.IO
import java.sql

trait ResultSetMetaDataFunctions extends DWorld[sql.ResultSetMetaData] {

  type ResultSetMetaData[+A] = Action[A]

  // TODO: ops

}