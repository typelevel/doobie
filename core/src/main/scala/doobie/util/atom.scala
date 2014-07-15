package doobie.util

import doobie.free._
import doobie.free.{ preparedstatement => PS }
import doobie.free.{ resultset => RS }
import doobie.enum.jdbctype._

import scalaz.InvariantFunctor
import scalaz.syntax.apply._
import scalaz.syntax.std.boolean._

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

    implicit def optionAtom[A](implicit A: Atom[A]): Atom[Option[A]] =
      new Atom[Option[A]] {
        def set = (n, a) => a.fold(setNull(n))(A.set(n, _))
        def get = n => (A.get(n) |@| RS.wasNull)((a, b) => b option a)
        val jdbcType = A.jdbcType
      }

    implicit val AtomInvariantFunctor: InvariantFunctor[Atom] =
      new InvariantFunctor[Atom] {
        def xmap[A,B](fa: Atom[A], f: A => B, g: B => A): Atom[B] =
          new Atom[B] {
            def set = (i, b) => fa.set(i, g(b))
            def get = i => fa.get(i).map(f)
            val jdbcType = fa.jdbcType
          }
      }

    def atom[A](jdbc: JdbcType, s: (Int, A) => PS.PreparedStatementIO[Unit], g: Int => RS.ResultSetIO[A]): Atom[A] =
      new Atom[A] {
        val set = s
        val get = g
        val jdbcType = jdbc
      }

  }

}