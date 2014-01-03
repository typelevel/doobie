package doobie
package hi

import dbc.{ preparedstatement => ps, resultset => rs, _ }

abstract class Prim[A](val jdbcType: JdbcType) {

  def set: (Int, A) => PreparedStatement[Unit]

  def get: Int => ResultSet[A]

  def setNull: Int => PreparedStatement[Unit] = i =>
    ps.setNull(i, jdbcType.toInt)

}

object Prim {

  def apply[A](implicit A: Prim[A]): Prim[A] = A

  def prim[A](jdbcType: JdbcType, s: (Int, A) => PreparedStatement[Unit], g: Int => ResultSet[A]): Prim[A] =
    new Prim[A](jdbcType) {
      val set = s
      val get = g
    }

  implicit val pInt: Prim[Int] =
    prim[Int](JdbcType.INTEGER, ps.setInt(_, _), rs.getInt(_))

  implicit val pStr: Prim[String] =
    prim[String](JdbcType.VARCHAR, ps.setString(_, _), rs.getString(_))

}