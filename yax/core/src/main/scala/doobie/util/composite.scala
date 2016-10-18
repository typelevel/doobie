
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

#+scalaz
import scalaz.InvariantFunctor
import scalaz.syntax.applicative._
#-scalaz
#+cats
import cats.functor.{ Invariant => InvariantFunctor }
import cats.implicits._
#-cats

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
#+scalaz
    def xmap[B](f: A => B, g: B => A): Composite[B] =
#-scalaz
#+cats
    def imap[B](f: A => B)(g: B => A): Composite[B] =
#-cats
      new Composite[B] {
        val set    = (n: Int, b: B) => c.set(n, g(b))
        val update = (n: Int, b: B) => c.update(n, g(b))
        val unsafeGet    = (r: ResultSet, n: Int) => f(c.unsafeGet(r,n))
        val length = c.length
        val meta   = c.meta
      }
  }

  object Composite extends LowerPriorityComposite with CompositesDerivations {

    def apply[A](implicit A: Composite[A]): Composite[A] = A

    implicit val compositeInvariantFunctor: InvariantFunctor[Composite] =
      new InvariantFunctor[Composite] {
#+scalaz
        def xmap[A, B](ma: Composite[A], f: A => B, g: B => A): Composite[B] =
          ma.xmap(f, g)
#-scalaz
#+cats
        def imap[A, B](ma: Composite[A])(f: A => B)(g: B => A): Composite[B] =
          ma.imap(f)(g)
#-cats
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
        val set = (i: Int, l: H :: T) => H.set(i, l.head) *> T.set(i + H.length, l.tail)
        val update = (i: Int, l: H :: T) => H.update(i, l.head) *> T.update(i + H.length, l.tail)
        val unsafeGet = (r: ResultSet, i: Int) => field[K](H.unsafeGet(r, i)) :: T.unsafeGet(r, i + H.length)
        val length = H.length + T.length
        val meta = H.meta ++ T.meta
      }

  }

  trait CompositesDerivations {
    import shapeless._
    import shapeless.ops.hlist._
    import tag.@@

    private[CompositesDerivations] sealed trait Required
    private[CompositesDerivations] object Required {
      private val tagger = new tag.Tagger[Required]
      def apply[T](t: T) = tagger(t)
    }

    private[CompositesDerivations] sealed trait Optional
    private[CompositesDerivations] object Optional {
      private val tagger = new tag.Tagger[Optional]
      def apply[T](t: T) = tagger(t)
    }

    trait LowPriorityOptionify extends Poly1 {
      implicit def caseDefault[A] =
        at[A] { a => Required(Option(a)) }
    }

    object optionify extends LowPriorityOptionify {
      implicit def caseOption[A] =
        at[Option[A]] { a => Optional(a) }
    }

    object mergeOptions extends Poly2 {
      implicit def caseOptional[H <: HList, A](implicit pre: Prepend[H, A :: HNil]) =
        at[Option[H], Option[A] @@ Required] { (oh, oa) =>
          for {
            h <- oh
            a <- oa
          } yield h :+ a
        }

      implicit def caseRequired[H <: HList, A](implicit pre: Prepend[H, Option[A] :: HNil]) =
        at[Option[H], Option[A] @@ Optional] { (oh, oa) =>
          for {
            h <- oh
          } yield h :+ oa.asInstanceOf[Option[A]]
        }
    }

    implicit def taggedCompositeRequired[T](implicit c: Composite[Option[T]]): Composite[Option[T] @@ Required] =
      Composite[Option[T]].xmap(Required.apply, _.asInstanceOf[Option[T]])

    implicit def taggedCompositeOptional[T](implicit c: Composite[Option[T]]): Composite[Option[T] @@ Optional] =
      Composite[Option[T]].xmap(Optional.apply, _.asInstanceOf[Option[T]])

    implicit def hlistGeneric[H <: HList] =
      new Generic[H] {
        type Repr = H
        def from(r: H): H = r
        def to(t: H): H = t
      }

    trait OptionalCompositeDerivation[A] {
      def apply[Repr <: HList, MOut <: HList, L <: Nat](empty: Option[A])(implicit
        gen: Lazy[Generic.Aux[A, Repr]],
        map: Lazy[Mapper.Aux[optionify.type, Repr, MOut]],
        comp: Lazy[Composite[MOut]],
        lf: Lazy[LeftFolder.Aux[MOut, Option[shapeless.HNil.type], mergeOptions.type, Option[Repr]]],
        length: Lazy[Length.Aux[Repr, L]],
        fill: Lazy[Fill[L, None.type]]
      ): Composite[Option[A]] = {
        implicit val folder = lf.value
        implicit val mapper = map.value
        val nones = fill.value(None).asInstanceOf[MOut]
        comp.value.xmap(
          { mo =>
            val res =
              mo.foldLeft(Option(HNil))(mergeOptions)
            // If all fields are Optional, and are all Nones,
            // use the empty value
            if (res == Option(nones)) empty
            else res.map(gen.value.from)
          },
          _.map { a =>
            gen.value.to(a).map(optionify)
          }.getOrElse(nones)
        )
      }
    }

    def optional[A] = new OptionalCompositeDerivation[A]{}
  }

  // N.B. we're separating this out in order to make the atom ~> composite derivation higher
  // priority than the product ~> composite derivation. So this means if we have an product mapped
  // to a single column, we will get only the atomic mapping, not the multi-column one.
  trait LowerPriorityComposite {

    implicit def product[H, T <: HList](implicit H: Composite[H], T: Composite[T]): Composite[H :: T] =
      new Composite[H :: T] {
        val set = (i: Int, l: H :: T) => H.set(i, l.head) *> T.set(i + H.length, l.tail)
        val update = (i: Int, l: H :: T) => H.update(i, l.head) *> T.update(i + H.length, l.tail)
        val unsafeGet = (r: ResultSet, i: Int) => H.unsafeGet(r, i) :: T.unsafeGet(r, i + H.length)
        val length = H.length + T.length
        val meta = H.meta ++ T.meta
      }

    implicit def emptyProduct: Composite[HNil] =
      new Composite[HNil] {
        val set = (_: Int, _: HNil) => ().pure[PS.PreparedStatementIO]
        val update = (_: Int, _: HNil) => ().pure[RS.ResultSetIO]
        val unsafeGet = (_: ResultSet, _: Int) => (HNil : HNil)
        val length = 0
        val meta = Nil
      }

    implicit def generic[F, G](implicit gen: Generic.Aux[F, G], G: Lazy[Composite[G]]): Composite[F] =
      new Composite[F] {
        val set: (Int, F) => PS.PreparedStatementIO[Unit] = (n, f) => G.value.set(n, gen.to(f))
        val update: (Int, F) => RS.ResultSetIO[Unit] = (n, f) => G.value.update(n, gen.to(f))
        val unsafeGet: (ResultSet, Int) => F = (rs, n) => gen.from(G.value.unsafeGet(rs, n))
        val length: Int = G.value.length
        val meta: List[(Meta[_], NullabilityKnown)] = G.value.meta
      }

  }

}
