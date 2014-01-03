package doobie

import dbc._
import scalaz.syntax.monad._

package object hi {

  def set[A](index: Int, a: A)(implicit A: Comp[A]): PreparedStatement[Unit] =
    A.set(index, a)

  def get[A](index: Int)(implicit A: Comp[A]): ResultSet[A] =
    A.get(index)

  def list[A](a: ResultSet[A]): ResultSet[List[A]] = {
    def list0(as: List[A]): ResultSet[List[A]] =
      resultset.next >>= {
        case false => as.point[ResultSet]
        case true  => a >>= { a => list0(a :: as) }
      }
    list0(Nil).map(_.reverse)
  }

}