package doobie.util

import doobie.free._
import doobie.free.{ preparedstatement => PS }
import doobie.free.{ resultset => RS }
import doobie.enum.jdbctype._
import doobie.enum.nullability._
import doobie.util.invariant._

import scalaz.{ Functor, Contravariant, InvariantFunctor, NonEmptyList }
import scalaz.syntax.apply._
import scalaz.syntax.std.boolean._

import java.sql.ParameterMetaData

import scala.reflect.runtime.universe.TypeTag

object atom {

  final case class JdbcMapping(
    primaryTarget:    JdbcType,
    secondaryTargets: List[JdbcType],
    primarySources:   NonEmptyList[JdbcType],
    secondaySources:  List[JdbcType]
  )

  /** 
   * Typeclass for types that can be represented in a single column. There are two subtypes, for 
   * unlifted (non-nullable) and lifted (nullable via Option) types.
   */
  sealed trait Atom[A] extends composite.Composite[A] { fa =>
    final val length = 1
    val jdbcMapping: JdbcMapping
    val get: Int => RS.ResultSetIO[A]
    val set: (Int, A) => PS.PreparedStatementIO[Unit]
    val update: (Int, A) => RS.ResultSetIO[Unit]
    val setNull: Int => PS.PreparedStatementIO[Unit] = i =>
      PS.setNull(i, jdbcMapping.primaryTarget.toInt)

    /** Tag for the innermost JDK type that defines the primitive mapping to JDBC. */
    def jdkType: TypeTag[_]
  }
  object Atom {
    def apply[A](implicit A: Atom[A]): Atom[A] = A
    implicit def lift[A](implicit A: Unlifted[A]): Atom[Option[A]] =
      A.lift
  }

  /**
   * `Atom` that permits reading and writing of SQL `NULL` via `Option`. Such atoms cannot be 
   * constructed directly, but can be obtained from an `Unlifted` via `.lift` or via the provided 
   * implicit.
   */
  final class Lifted[A] private[atom] (
      j: JdbcMapping,
      g: Int => RS.ResultSetIO[A],
      s: (Int, A) => PS.PreparedStatementIO[Unit],
      u: (Int, A) => RS.ResultSetIO[Unit],
      val jdkType: TypeTag[_]
  ) extends Atom[Option[A]] {
    val jdbcMapping = j
    val get = (n: Int) => ^(g(n), RS.wasNull)((a, b) => (!b) option a)
    val set = (n: Int, a: Option[A]) => a.fold(setNull(n))(s(n, _))
    val update = (n: Int, a: Option[A]) => a.fold(RS.updateNull(n))(u(n, _))
    def xmap[B](ab: A => B, ba: B => A): Lifted[B] =
      new Lifted[B](j, n => g(n).map(ab), (n, b) => s(n, ba(b)), (n, b) => u(n, ba(b)), jdkType)
    def ameta = List(analysis.JdkMeta(jdbcMapping.primaryTarget, Nullable, jdkType))
  }
  object Lifted {
    def apply[A](implicit A: Lifted[A]): Lifted[A] = A
    implicit def lift[A](implicit A: Unlifted[A]): Lifted[A] =
      A.lift
  }

  /** 
   * Atom that does not permit reading or writing of SQL `NULL`. This is the only type of `Atom` (or
   * `Composite`) that can be constructed directly. Implicit instances are provided for ground types
   * in the `std` package.
   */
  final case class Unlifted[A](lift: Lifted[A]) extends Atom[A] { fa =>
    val jdbcMapping = lift.jdbcMapping
    val get = (n: Int) => lift.get(n).map(_.getOrElse(throw NonNullableColumnRead(n, jdbcMapping.primaryTarget)))
    val set = (n: Int, a: A) => lift.set(n, if (a == null) throw NonNullableParameter(n, jdbcMapping.primaryTarget) else Some(a))
    val update = (n: Int, a: A) => lift.update(n, if (a == null) throw NonNullableColumnUpdate(n, jdbcMapping.primaryTarget) else Some(a))
    def xmap[B](ab: A => B, ba: B => A): Unlifted[B] = Unlifted(lift.xmap(ab, ba))
    def jdkType = lift.jdkType
    def ameta =List(analysis.JdkMeta(jdbcMapping.primaryTarget, NoNulls, jdkType))
  }
  object Unlifted {
    def apply[A](implicit A: Unlifted[A]): Unlifted[A] = A

    /** 
     * Construct an `Unlifted` atom for the given type. Such instances are typically marked as
     * implicit to allow automatic derivation of the `Lifted` variant, as well as larger 
     * `Composite` instances that contain `A` or `Option[A]` as a field.
     */
    def create[A](
        j: JdbcType, 
        s: (Int, A) => PS.PreparedStatementIO[Unit], 
        u: (Int, A) => RS.ResultSetIO[Unit],
        g: Int => RS.ResultSetIO[A])(implicit tt: TypeTag[A]): Unlifted[A] = 
      Unlifted(new Lifted(JdbcMapping(j, Nil, NonEmptyList(j), Nil), g, s, u, tt))
      
  }

}





