package doobie.util

import doobie.free._
import doobie.free.{ preparedstatement => PS }
import doobie.free.{ resultset => RS }
import doobie.enum.jdbctype._
import doobie.enum.nullability._
import doobie.enum.scalatype._
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







// object atom_old {

//   final case class JdbcMapping(
//     primaryTarget:    JdbcType,
//     secondaryTargets: List[JdbcType],
//     primarySources:   NonEmptyList[JdbcType],
//     secondaySources:  List[JdbcType]
//   )

//   /** 
//    * Typeclass for types that can be represented in a single column. There are two subtypes, for 
//    * unlifted (non-nullable) and lifted (nullable via Option) types.
//    */
//   sealed trait Atom[A] extends composite.Composite[A] { fa =>
//     final val length = 1
//     val jdbcMapping: JdbcMapping
//     val get: Int => RS.ResultSetIO[A]
//     val set: (Int, A) => PS.PreparedStatementIO[Unit]
//     val update: (Int, A) => RS.ResultSetIO[Unit]
//     val setNull: Int => PS.PreparedStatementIO[Unit] = i =>
//       PS.setNull(i, jdbcMapping.primaryTarget.toInt)

//     /** Tag for the innermost JDK type that defines the primitive mapping to JDBC. */
//     def jdkType: TypeTag[_]
//   }
//   object Atom {
//     def apply[A](implicit A: Atom[A]): Atom[A] = A
//     implicit def lift[A](implicit A: Unlifted[A]): Atom[Option[A]] =
//       A.lift
//   }

//   /**
//    * `Atom` that permits reading and writing of SQL `NULL` via `Option`. Such atoms cannot be 
//    * constructed directly, but can be obtained from an `Unlifted` via `.lift` or via the provided 
//    * implicit.
//    */
//   final class Lifted[A] private[atom] (
//       j: JdbcMapping,
//       g: Int => RS.ResultSetIO[A],
//       s: (Int, A) => PS.PreparedStatementIO[Unit],
//       u: (Int, A) => RS.ResultSetIO[Unit],
//       val jdkType: TypeTag[_]
//   ) extends Atom[Option[A]] {
//     val jdbcMapping = j
//     val get = (n: Int) => ^(g(n), RS.wasNull)((a, b) => (!b) option a)
//     val set = (n: Int, a: Option[A]) => a.fold(setNull(n))(s(n, _))
//     val update = (n: Int, a: Option[A]) => a.fold(RS.updateNull(n))(u(n, _))
//     def xmap[B](ab: A => B, ba: B => A): Lifted[B] =
//       new Lifted[B](j, n => g(n).map(ab), (n, b) => s(n, ba(b)), (n, b) => u(n, ba(b)), jdkType)
//     def ameta = List(analysis.JdkMeta(jdbcMapping, Nullable, jdkType))
//   }
//   object Lifted {
//     def apply[A](implicit A: Lifted[A]): Lifted[A] = A
//     implicit def lift[A](implicit A: Unlifted[A]): Lifted[A] =
//       A.lift
//   }

//   /** 
//    * Atom that does not permit reading or writing of SQL `NULL`. This is the only type of `Atom` (or
//    * `Composite`) that can be constructed directly. Implicit instances are provided for ground types
//    * in the `std` package.
//    */
//   final case class Unlifted[A](lift: Lifted[A]) extends Atom[A] { fa =>
//     val jdbcMapping = lift.jdbcMapping
//     val get = (n: Int) => lift.get(n).map(_.getOrElse(throw NonNullableColumnRead(n, jdbcMapping.primaryTarget)))
//     val set = (n: Int, a: A) => lift.set(n, if (a == null) throw NonNullableParameter(n, jdbcMapping.primaryTarget) else Some(a))
//     val update = (n: Int, a: A) => lift.update(n, if (a == null) throw NonNullableColumnUpdate(n, jdbcMapping.primaryTarget) else Some(a))
//     def xmap[B](ab: A => B, ba: B => A): Unlifted[B] = Unlifted(lift.xmap(ab, ba))
//     def jdkType = lift.jdkType
//     def ameta =List(analysis.JdkMeta(jdbcMapping, NoNulls, jdkType))
//   }
//   object Unlifted {
//     def apply[A](implicit A: Unlifted[A]): Unlifted[A] = A

//     /** 
//      * Construct an `Unlifted` atom for the given type. Such instances are typically marked as
//      * implicit to allow automatic derivation of the `Lifted` variant, as well as larger 
//      * `Composite` instances that contain `A` or `Option[A]` as a field.
//      */
//     def create[A](
//         j: JdbcType, 
//         s: (Int, A) => PS.PreparedStatementIO[Unit], 
//         u: (Int, A) => RS.ResultSetIO[Unit],
//         g: Int => RS.ResultSetIO[A])(implicit tt: TypeTag[A]): Unlifted[A] = 
//       Unlifted(new Lifted(JdbcMapping(j, Nil, NonEmptyList(j), Nil), g, s, u, tt))
      
//   }

// }
