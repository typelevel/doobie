package doobie.util

import doobie.free._
import doobie.free.{ preparedstatement => PS }
import doobie.free.{ resultset => RS }
import doobie.enum.jdbctype._
import doobie.enum.nullability._
import doobie.util.scalatype._
import doobie.util.invariant._

import scalaz.{ Functor, Contravariant, InvariantFunctor, NonEmptyList }
import scalaz.std.option._
import scalaz.syntax.applicative._
import scalaz.syntax.std.boolean._

import java.sql.ParameterMetaData

import scala.annotation.implicitNotFound
import scala.reflect.runtime.universe.TypeTag

object atom {

  /** 
   * A `ScalaType` together with a `Nullability` and `NULL`-aware `get/set/update`. Given an 
   * implicit `ScalaType[A]` you get derived `Atom[A]` and `Atom[Option[A]]`.
   */
  @implicitNotFound("Could not find or construct Atom[${A}]; ensure that A has a ScalaType instance.")
  sealed trait Atom[A] { outer => 
    val set: (Int, A) => PS.PreparedStatementIO[Unit]
    val update: (Int, A) => RS.ResultSetIO[Unit]
    val get: Int => RS.ResultSetIO[A]
    val meta: (ScalaType[_], NullabilityKnown)
    def xmap[B](f: A => B, g: B => A): Atom[B] =
      new Atom[B] {
        val set = (n: Int, b: B) => outer.set(n, g(b))
        val update = (n: Int, b: B) => outer.update(n, g(b))
        val get = (n: Int) => outer.get(n).map(f)
        val meta = outer.meta
      }
  }
  object Atom {

    def apply[A](implicit A: Atom[A]): Atom[A] = A

    implicit def fromScalaType[A](implicit A: ScalaType[A]): Atom[A] = 
      new Atom[A] {
        val get = (n: Int) => ^(A.get(n), RS.wasNull)((a, b) => if (b) throw NonNullableColumnRead(n, A.primaryTarget) else a)
        val set = (n: Int, a: A) => if (a == null) throw NonNullableParameter(n, A.primaryTarget) else A.set(n, a)
        val update = (n: Int, a: A) => if (a == null) throw NonNullableColumnUpdate(n, A.primaryTarget) else A.update(n, a)
        val meta = (A, NoNulls)
      }

    implicit def fromScalaTypeOption[A](implicit A: ScalaType[A]): Atom[Option[A]] = 
      new Atom[Option[A]] {
        val get = (n: Int) => ^(A.get(n), RS.wasNull)((a, b) => (!b) option a)
        val set = (n: Int, a: Option[A]) => a.fold(PS.setNull(n, A.primaryTarget.toInt))(A.set(n, _))
        val update = (n: Int, a: Option[A]) => a.fold(RS.updateNull(n))(A.update(n, _))
        val meta = (A, Nullable)
      }

  }
  
}
