package doobie
package dbc

import scalaz.effect.IO
import java.sql

trait ParameterMetaDataFunctions extends DWorld[sql.ParameterMetaData] {

  type ParameterMetaData[+A] = Action[A]

  // TODO: ops

}