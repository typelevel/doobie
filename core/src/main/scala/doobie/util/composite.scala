
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

import scala.annotation.implicitNotFound

import scalaz._, Scalaz._
import shapeless._

import java.sql.ParameterMetaData

/**
 * Module defining a typeclass for composite database types (those that can map to multiple columns).
 */
object composite {

  @implicitNotFound("Could not find or construct Composite[${A}].")
  trait Composite[A] { c =>
    val set: (Int, A) => PS.PreparedStatementIO[Unit]
    val update: (Int, A) => RS.ResultSetIO[Unit]
    val get: Int => RS.ResultSetIO[A]
    val length: Int
    val meta: List[(Meta[_], NullabilityKnown)]
    final def xmap[B](f: A => B, g: B => A): Composite[B] =
      new Composite[B] {
        val set    = (n: Int, b: B) => c.set(n, g(b))
        val update = (n: Int, b: B) => c.update(n, g(b))
        val get    = (n: Int) => c.get(n).map(f)
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
        val get = A.get
        val length = 1
        val meta = List(A.meta)
      }

    implicit def hlistComposite[H, T <: HList](implicit H: Composite[H], T: Lazy[Composite[T]]): Composite[H :: T] =
      new Composite[H :: T] {
        val set = (i: Int, l: H :: T) => H.set(i, l.head) >> T.value.set(i + H.length, l.tail)
        val update = (i: Int, l: H :: T) => H.update(i, l.head) >> T.value.update(i + H.length, l.tail)
        val get = (i: Int) => (H.get(i) |@| T.value.get(i + H.length))(_ :: _)
        val length = H.length + T.value.length
        val meta = H.meta ++ T.value.meta
      }

    implicit val hnilComposite: Composite[HNil] =
      new Composite[HNil] {
        val set = (_: Int, _: HNil) => ().point[PS.PreparedStatementIO]
        val update = (_: Int, _: HNil) => ().point[RS.ResultSetIO]
        val get = (_: Int) => (HNil : HNil).point[RS.ResultSetIO]
        val length = 0
        val meta = Nil
      }
  }

  // N.B. we're separating this out in order to make the atom ~> composite derivation higher
  // priority than the product ~> composite derivation. So this means if we have an product mapped
  // to a single column, we will get only the atomic mapping, not the multi-column one.
  trait LowerPriorityComposite {
    implicit def genericComposite[A, R](implicit gen: Generic.Aux[A, R], rc: Lazy[Composite[R]]): Composite[A] =
      new Composite[A] {
        val set = (n: Int, a: A) => rc.value.set(n, gen.to(a))
        val update = (n: Int, a: A) => rc.value.update(n, gen.to(a))
        val get = (n: Int) => rc.value.get(n).map(gen.from)
        val length = rc.value.length
        val meta = rc.value.meta
      }
  }
}
