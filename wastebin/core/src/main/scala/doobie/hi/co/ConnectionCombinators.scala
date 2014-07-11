package doobie
package hi
package co

import java.sql
import dbc.op.ConnectionOps
import scalaz._
import Scalaz._
import scalaz.effect.{IO, MonadIO}
import scalaz.syntax.effect.monadCatchIO._
import scalaz.stream._

trait ConnectionCombinators 
  extends ConnectionOps
  with ProcessPrimitives[sql.Connection] {

  def process[A: Comp](sql: String, configure: preparedstatement.Action[Unit]): Process[Action, A] = {

    val prepAndExec = 
      configure >> preparedstatement.primitive("executeQuery", _.executeQuery)
      
    def acquire: Action[java.sql.ResultSet] = 
      push("acquire") {
        for {
          ps <- primitive("prepareStatement", _.prepareStatement(sql))
          rs <- gosub0(ps.point[Action], prepAndExec)
        } yield rs
      }

    // TODO: close PS

    resource[java.sql.ResultSet, A](
      acquire)(rs =>
      gosub0(rs.point[Action], resultset.close, "release"))(rs =>
      gosub0(rs.point[Action], resultset.getNext[A], "step"))

  } 

}

