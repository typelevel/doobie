package doobie

import dbc._
import scalaz.syntax.monad._
import scalaz.effect.IO

package object hi {

  def set[A](index: Int, a: A)(implicit A: Comp[A]): PreparedStatement[Unit] =
    preparedstatement.push(s"set[«erased»]($index, $a)", A.set(index, a))

  def get[A](index: Int)(implicit A: Comp[A]): ResultSet[A] =
    resultset.push(s"get[«erased»]($index)", A.get(index))

  def list[A: Comp]: ResultSet[List[A]] = {
    def list0(as: List[A]): ResultSet[List[A]] =
      resultset.next >>= {
        case false => as.point[ResultSet]
        case true  => get[A](1) >>= { a => list0(a :: as) }
      }
    list0(Nil).map(_.reverse)
  }

  def sink[A: Comp](effect: A => IO[Unit]): ResultSet[Unit] =
    resultset.next >>= {
      case false => ().point[ResultSet]
      case true  => (get[A](1) >>= { a => effect(a).liftIO[ResultSet] }) >> sink(effect)
    }

 import connection.prepareStatement

  implicit class SqlInterpolator(val sc: StringContext) {

    class Source[A: Comp](a: A) {

      def go[B](b: PreparedStatement[B]): Connection[B] =
        prepareStatement(sc.parts.mkString("?"))(set(1, a) >> b)

      def executeQuery[B](b: ResultSet[B]): Connection[B] =
        go(preparedstatement.executeQuery(b))
    
      def execute: Connection[Boolean] =
        go(preparedstatement.execute)

    }

    def sql[A: Prim](a: A) = new Source(a)

    def sql[A: Prim, B: Prim](a: A, b: B) = new Source((a,b))

  }
  
}