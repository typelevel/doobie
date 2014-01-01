package doobie
package dbc

import scalaz.effect.IO
import java.sql

object callablestatement extends util.TWorld[sql.CallableStatement] with PreparedStatementOps[sql.CallableStatement] {

  type CallableStatement[+A] = Action[A]

  private[dbc] def run[A](a: CallableStatement[A], s: sql.CallableStatement): IO[A] = 
    eval(a, s).map(_._2)
  
}
