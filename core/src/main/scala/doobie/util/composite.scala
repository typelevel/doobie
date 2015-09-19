
package doobie.util

import doobie.enum.jdbctype.JdbcType
import doobie.util.meta.Meta
import doobie.enum.nullability._
import doobie.util.atom._
import doobie.util.invariant._
import doobie.free.resultset.{ ResultSetIO, updateNull }
import doobie.free.preparedstatement.PreparedStatementIO
import doobie.free.{ preparedstatement => PS }
import doobie.free.{ resultset => RS }

import java.sql.ResultSet

import scala.annotation.implicitNotFound

import scalaz._, Scalaz._
import shapeless._
import shapeless.labelled.{ field, FieldType }

import java.sql.ParameterMetaData

/**
 * Module defining a typeclass for composite database types (those that can map to multiple columns).
 */
object composite {

  @implicitNotFound("""Could not find or construct Composite[${A}]. 
Ensure that this type has a Composite instance in scope; or is a Product type whose members have 
Composite instances in scope; or is an atomic type with an Atom instance in scope. You can usually 
diagnose this problem by trying to summon the Composite instance for each element in the REPL. See 
the FAQ in the Book of Doobie for more hints.""")
  trait Composite[A] { c =>
    val set: (Int, A) => PS.PreparedStatementIO[Unit]
    val update: (Int, A) => RS.ResultSetIO[Unit]
    val get: Int => RS.ResultSetIO[A] = n => RS.raw(rs => unsafeGet(rs, n))
    val unsafeGet: (ResultSet, Int) => A
    val length: Int
    val meta: List[(Meta[_], NullabilityKnown)]
    final def xmap[B](f: A => B, g: B => A): Composite[B] =
      new Composite[B] {
        val set    = (n: Int, b: B) => c.set(n, g(b))
        val update = (n: Int, b: B) => c.update(n, g(b))
        val unsafeGet    = (r: ResultSet, n: Int) => f(c.unsafeGet(r,n))
        val length = c.length
        val meta   = c.meta
      }
  }

  object Composite extends LowerPriorityComposite {

    def apply[A](implicit A: Composite[A]): Composite[A] = A

    implicit val compositeInvariantFunctor: InvariantFunctor[Composite] =
      new InvariantFunctor[Composite] {
        def xmap[A, B](ma: Composite[A], f: A => B, g: B => A): Composite[B] =
          ma.xmap(f, g)
      }

    implicit def fromAtom[A](implicit A: Atom[A]): Composite[A] =
      new Composite[A] {
        val set = A.set
        val update = A.update
        val unsafeGet = A.unsafeGet
        val length = 1
        val meta = List(A.meta)
      }

    // Composite for shapeless record types
    implicit def recordComposite[K <: Symbol, H, T <: HList](implicit H: Composite[H], T: Composite[T]): Composite[FieldType[K, H] :: T] =
      new Composite[FieldType[K, H] :: T] {
        val set = (i: Int, l: H :: T) => H.set(i, l.head) >> T.set(i + H.length, l.tail)
        val update = (i: Int, l: H :: T) => H.update(i, l.head) >> T.update(i + H.length, l.tail)
        val unsafeGet = (r: ResultSet, i: Int) => field[K](H.unsafeGet(r, i)) :: T.unsafeGet(r, i + H.length)
        val length = H.length + T.length
        val meta = H.meta ++ T.meta
      }

  }

  // N.B. we're separating this out in order to make the atom ~> composite derivation higher
  // priority than the product ~> composite derivation. So this means if we have an product mapped
  // to a single column, we will get only the atomic mapping, not the multi-column one.
  trait LowerPriorityComposite extends ProductTypeClassCompanion[Composite] {

    /** @group Typeclass Instances */
    object typeClass extends ProductTypeClass[Composite] {

      def product[H, T <: HList](H: Composite[H], T: Composite[T]): Composite[H :: T] =
        new Composite[H :: T] {
          val set = (i: Int, l: H :: T) => H.set(i, l.head) >> T.set(i + H.length, l.tail)
          val update = (i: Int, l: H :: T) => H.update(i, l.head) >> T.update(i + H.length, l.tail)
          val unsafeGet = (r: ResultSet, i: Int) => H.unsafeGet(r, i) :: T.unsafeGet(r, i + H.length)
          val length = H.length + T.length
          val meta = H.meta ++ T.meta
        }

      def emptyProduct: Composite[HNil] =
        new Composite[HNil] {
          val set = (_: Int, _: HNil) => ().point[PS.PreparedStatementIO]
          val update = (_: Int, _: HNil) => ().point[RS.ResultSetIO]
          val unsafeGet = (_: ResultSet, _: Int) => (HNil : HNil)
          val length = 0
          val meta = Nil
        }

      def project[F, G](instance: => Composite[G], to: F => G, from: G => F): Composite[F] =
        instance.xmap(from, to)
    }

  }

}
