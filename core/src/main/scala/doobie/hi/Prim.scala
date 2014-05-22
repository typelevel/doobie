package doobie
package hi

import dbc.{ preparedstatement => ps, resultset => rs, _ }
import dbc.enum._
import scalaz.InvariantFunctor

abstract class Prim[A](val jdbcType: JdbcType) { outer =>

  def set: (Int, A) => PreparedStatement[Unit]

  def get: Int => ResultSet[A]

  def setNull: Int => PreparedStatement[Unit] = i =>
    ps.setNull(i, jdbcType.toInt)

}

object Prim {

  def apply[A](implicit A: Prim[A]): Prim[A] = A

  implicit val invariantFunctor: InvariantFunctor[Prim] =
    new InvariantFunctor[Prim] {
      def xmap[A,B](fa: Prim[A], f: A => B, g: B => A): Prim[B] =
        new Prim[B](fa.jdbcType) {
          def set = (i, b) => fa.set(i, g(b))
          def get = i => fa.get(i).map(f)
        }
    }

  // todo: generate Prim[A] and Prim[Option[A]] pairs, both for reference types (need to check null 
  // values) and value types (need to call wasNull). Throw UnmetInvariantException or something on
  // unexpected null.

  def prim[A](jdbcType: JdbcType, s: (Int, A) => PreparedStatement[Unit], g: Int => ResultSet[A]): Prim[A] =
    new Prim[A](jdbcType) {
      val set = s
      val get = g
    }

  implicit val pInt = prim[Int](JdbcType.Integer, ps.setInt, rs.getInt) 
  implicit val pStr = prim[String](JdbcType.VarChar, ps.setString, rs.getString)
  implicit val pDouble = prim[Double](JdbcType.Real, ps.setDouble, rs.getDouble)

}