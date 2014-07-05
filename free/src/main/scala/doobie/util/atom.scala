package doobie.util

import doobie.hi._
import doobie.enum.jdbctype._

import scalaz.InvariantFunctor

/** 
 * Module defining a typeclass for atomic database types (those that map to a single column).
 */
object atom {

  trait Atom[A] {

    def set: (Int, A) => PreparedStatementIO[Unit]

    def get: Int => ResultSetIO[A]

    def jdbcType: JdbcType

    def setNull: Int => PreparedStatementIO[Unit] = i =>
      preparedstatement.setNull(i, jdbcType.toInt)

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

    def prim[A](jdbc: JdbcType, s: (Int, A) => PreparedStatementIO[Unit], g: Int => ResultSetIO[A]): Atom[A] =
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