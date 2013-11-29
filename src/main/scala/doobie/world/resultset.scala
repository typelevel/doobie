package doobie
package world

import doobie.util._
import doobie._
import java.sql.ResultSet
import scalaz._
import Scalaz._
import scalaz.stream._
import scalaz.concurrent._

object resultset extends DWorld.Indexed {

  protected type R = ResultSet

  ////// PRIMITIVE OPS

  def readN[A](n: Int)(implicit A: Primitive[A]): Action[A] =
    asks(A.get(_)(n)) :++>> (a => s"GET $n ${A.jdbcType.name} => $a")

  def wasNull: Action[Boolean] =
    asks(_.wasNull) :++>> (a => s"WAS NULL => $a")

  def next: Action[Boolean] =
    asks(_.next) :++>> (a => s"NEXT => $a")

  ////// INDEXED OPS

  def read[A: Primitive]: Action[A] =
    get >>= (n => readN[A](n))

  ////// STREAM OPS

  // Placeholder for stream-based reader
  def list[O](implicit O: Composite[O]): Action[List[O]] =
    next >>= { 
      case false => success(Nil)
      case true  => reset >> O.get >>= (h => list.map(h :: _))
    }

  // def stream[O](implicit O : Composite[O]): Action[Vector[O]] =
  //   asks { rs =>
  //     val p = Process.eval { 
  //       if (rs.next) {
  //         Task.delay(runi(rs, O.get))

  //         } else 
  //         throw Process.End
  //     }
  //     p.runLog.map(_.toVector).run
  //   }

  ////// LIFTING

  def lift[A](a: Action[A]): statement.Action[A] =
    statement.executeQuery(runi(_, a))

  ////// SYNTAX

  implicit class ResultSetActionOps[A](a: Action[A]) {
    def lift: statement.Action[A] =
      resultset.lift(a)
  }

}

