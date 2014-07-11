package doobie.util

import doobie.free._
import doobie.free.{ preparedstatement => PS }
import doobie.free.{ resultset => RS }
import doobie.enum.jdbctype._

import scalaz.InvariantFunctor

/** 
 * Module defining a typeclass for atomic database types (those that map to a single column).
 */
object atom {

  trait Atom[A] {

    def set: (Int, A) => PS.PreparedStatementIO[Unit]

    def get: Int => RS.ResultSetIO[A]

    def jdbcType: JdbcType

    def setNull: Int => PS.PreparedStatementIO[Unit] = i =>
      PS.setNull(i, jdbcType.toInt)

  }

  object Atom {

    def apply[A](implicit A: Atom[A]): Atom[A] = A

    implicit val invariantFunctor: InvariantFunctor[Atom] =
      new InvariantFunctor[Atom] {
        def xmap[A,B](fa: Atom[A], f: A => B, g: B => A): Atom[B] =
          new Atom[B] {
            def set = (i, b) => fa.set(i, g(b))
            def get = i => fa.get(i).map(f)
            val jdbcType = fa.jdbcType
          }
      }

    // todo: generate Atom[A] and Atom[Option[A]] pairs, both for reference types (need to check null 
    // values) and value types (need to call wasNull). Throw UnmetInvariantException or something on
    // unexpected null.

    def atom[A](jdbc: JdbcType, s: (Int, A) => PS.PreparedStatementIO[Unit], g: Int => RS.ResultSetIO[A]): Atom[A] =
      new Atom[A] {
        val set = s
        val get = g
        val jdbcType = jdbc
      }

    // implicit val pInt = prim[Int](JdbcType.Integer, ps.setInt, rs.getInt) 
    // implicit val pStr = prim[String](JdbcType.VarChar, ps.setString, rs.getString)
    // implicit val pDouble = prim[Double](JdbcType.Real, ps.setDouble, rs.getDouble)

  }

}