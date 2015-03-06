
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
  // contexts for current index
  type PreparedStatementContext[A] = StateT[PS.PreparedStatementIO, Int, A]
  type ResultSetContext[A] = StateT[RS.ResultSetIO, Int, A]

  @implicitNotFound("Could not find or construct Composite[${A}].")
  trait Composite[A] { c =>
    val set: A => PreparedStatementContext[Unit]
    val update: A => ResultSetContext[Unit]
    val get: ResultSetContext[A]
    val meta: List[(Meta[_], NullabilityKnown)]
    final def xmap[B](f: A => B, g: B => A): Composite[B] =
      new Composite[B] {
        val set = (b: B) => c.set(g(b))
        val update = (b: B) => c.update(g(b))
        val get = c.get map f
        val meta = c.meta
      }
  }

  object Composite extends LowerPriorityComposite with HListComposite {

    def apply[A](implicit A: Composite[A]): Composite[A] = A

    implicit val compositeInvariantFunctor: InvariantFunctor[Composite] =
      new InvariantFunctor[Composite] {
        def xmap[A, B](ma: Composite[A], f: A => B, g: B => A): Composite[B] =
          ma.xmap(f, g)
      }

    implicit def fromAtom[A](implicit A: Atom[A]): Composite[A] =
      new Composite[A] {
        val set = (a: A) => StateT[PS.PreparedStatementIO, Int, Unit](s => A.set(s, a) map ((s + 1, _)))
        val update = (a: A) => StateT[RS.ResultSetIO, Int, Unit](s => A.update(s, a).map((s + 1, _)))
        val get = StateT[RS.ResultSetIO, Int, A](s => A.get(s).map((s + 1, _)))
        val meta = List(A.meta)
      }
  }

  // N.B. we're separating this out in order to make the atom ~> composite derivation higher
  // priority than the product ~> composite derivation. So this means if we have an product mapped
  // to a single column, we will get only the atomic mapping, not the multi-column one.
  trait LowerPriorityComposite {

    /** @group Typeclass Instances */
    implicit val productComposite: ProductTypeClass[Composite] =
      new ProductTypeClass[Composite] {

        def product[H, T <: HList](H: Composite[H], T: Composite[T]): Composite[H :: T] =
          HListComposite.hlistComposite[H, T](H, T)

        def emptyProduct: Composite[HNil] = HListComposite.hnilComposite

        def project[F, G](instance: => Composite[G], to: F => G, from: G => F): Composite[F] =
          instance.xmap(from, to)
      }

    /** @group Typeclass Instances */
    implicit def deriveComposite[T](implicit ev: ProductTypeClass[Composite]): Composite[T] =
      macro GenericMacros.deriveProductInstance[Composite, T]
  }

  object HListComposite extends HListComposite

  trait HListComposite {
    implicit def hlistComposite[H, T <: HList](implicit H: Composite[H], T: Composite[T]): Composite[H :: T] =
      new Composite[H :: T] {
        val set = (l: H :: T) => H.set(l.head) >> T.set(l.tail)
        val update = (l: H :: T) => H.update(l.head) >> T.update(l.tail)
        val get = H.get >>= (h => T.get map (t => (h :: t)))
        val meta = H.meta ++ T.meta
      }

    implicit val hnilComposite: Composite[HNil] =
      new Composite[HNil] {
        val set = (_: HNil) => ().point[PreparedStatementContext]
        val update = (_: HNil) => ().point[ResultSetContext]
        val get = (HNil : HNil).point[ResultSetContext]
        val meta = Nil
      }
  }
}
