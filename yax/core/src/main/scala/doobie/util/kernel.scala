package doobie.util

import java.sql.{ ResultSet, PreparedStatement }

#+scalaz
import scalaz.InvariantFunctor
#-scalaz
#+cats
import cats.functor.{ Invariant => InvariantFunctor }
#-cats

import shapeless._
import shapeless.labelled.{ field, FieldType }

object kernel {

  /**
   * A data type for the common internal structure of Meta and Composite. This is not a
   * typeclass. We bake in contravariant coyoneda to enable proper null handling in the Meta case;
   * in the other cases it's less important but the consistency is probably worthwhile. Note that
   * although the operations here are unsafe, their composition is lawful.
   */
  trait Kernel[A] { outer =>

    type I
    val ia: I => A
    val ai: A => I
    val get: (ResultSet, Int) => I
    val set: (PreparedStatement, Int, I) => Unit
    val setNull: (PreparedStatement, Int) => Unit
    val update: (ResultSet, Int, I) => Unit
    val width: Int

#+scalaz
    def xmap[B](f: A => B, g: B => A): Kernel[B] =
#-scalaz
#+cats
    def imap[B](f: A => B)(g: B => A): Kernel[B] =
#-cats
      new Kernel[B] {
        type I      = outer.I
        val ia      = outer.ia andThen f
        val ai      = outer.ai compose g
        val get     = outer.get
        val set     = outer.set
        val setNull = outer.setNull
        val update  = outer.update
        val width   = outer.width
      }

    def zip[B](b: Kernel[B]): Kernel[(A, B)] =
      Kernel.product(this, b)

  }

  object Kernel extends KernelInstances {

    val unit: Kernel[Unit] =
      new Kernel[Unit] {
        type I      = Unit
        val ia      = (i: I) => i
        val ai      = (a: I) => a
        val get     = (_: ResultSet, _: Int) => ()
        val set     = (_: PreparedStatement, _: Int, _: I) => ()
        val setNull = (_: PreparedStatement, _: Int) => ()
        val update  = (_: ResultSet, _: Int, _: Unit) => ()
        val width   = 0
      }

    val hnil: Kernel[HNil] =
      new Kernel[HNil] {
        type I      = HNil
        val ia      = (i: I) => i
        val ai      = (a: I) => a
        val get     = (_: ResultSet, _: Int) => HNil
        val set     = (_: PreparedStatement, _: Int, _: I) => ()
        val setNull = (_: PreparedStatement, _: Int) => ()
        val update  = (_: ResultSet, _: Int, _: HNil) => ()
        val width   = 0
      }

    def hcons[H, T <: HList](h: Kernel[H], t: Kernel[T]): Kernel[H :: T] =
      new Kernel[H :: T] {
        type I      = H :: T
        val ia      = (i: I) => i
        val ai      = (a: I) => a
        val get     = (rs: ResultSet, n: Int)     =>   h.ia(h.get(rs, n)) ::            t.ia(t.get(rs, n + h.width))
        val set     = (ps: PreparedStatement, n: Int, ht: I) => { h.set(ps, n, h.ai(ht.head)) ;    t.set(ps, n + h.width, t.ai(ht.tail)) }
        val setNull = (ps: PreparedStatement, n: Int)     => { h.setNull(ps, n) ;               t.setNull(ps, n + h.width) }
        val update  = (rs: ResultSet, n: Int, ht: I) => { h.update(rs, n, h.ai(ht.head)) ; t.update(rs, n + h.width, t.ai(ht.tail)) }
        val width   = h.width + t.width
      }

    def product[A, B](a: Kernel[A], b: Kernel[B]): Kernel[(A, B)] =
      new Kernel[(A, B)] {
        type I      = (A, B)
        val ia      = (i: I) => i
        val ai      = (a: I) => a
        val get     = (rs: ResultSet, n: Int) => ( a.ia(a.get(rs, n)), b.ia(b.get(rs, n + a.width)) )
        val set     = (ps: PreparedStatement, n: Int, ab: I) => { a.set(ps, n, a.ai(ab._1)); b.set(ps, n + a.width, b.ai(ab._2)) }
        val setNull = (ps: PreparedStatement, n: Int) => { a.setNull(ps, n); b.setNull(ps, n + a.width) }
        val update  = (rs: ResultSet, n: Int, ab: I) => { a.update(rs, n, a.ai(ab._1)) ; b.update(rs, n + a.width, b.ai(ab._2)) }
        val width   = a.width + b.width
      }

    def record[K <: Symbol, H, T <: HList](h: Kernel[H], t: Kernel[T]): Kernel[FieldType[K, H] :: T] =
      new Kernel[FieldType[K, H] :: T] {
        type I      = FieldType[K, H] :: T
        val ia      = (i: I) => i
        val ai      = (a: I) => a
        val get     = (rs: ResultSet, n: Int) => field[K](h.ia(h.get(rs, n))) :: t.ia(t.get(rs, n + h.width))
        val set     = (ps: PreparedStatement, n: Int, ht: I) => { h.set(ps, n, h.ai(ht.head)); t.set(ps, n + h.width, t.ai(ht.tail)) }
        val setNull = (ps: PreparedStatement, n: Int) => { h.setNull(ps, n);  t.setNull(ps, n + h.width) }
        val update  = (rs: ResultSet, n: Int, ht: I) => { h.update(rs, n, h.ai(ht.head)); t.update(rs, n + h.width, t.ai(ht.tail)) }
        val width   = h.width + t.width
      }

  }

  trait KernelInstances {
    // TODO: InvariantFunctor (scalaz), Invariant and Cartesian (cats)

    implicit val kernelInvariantFunctor: InvariantFunctor[Kernel] =
      new InvariantFunctor[Kernel] {
#+scalaz
        def xmap[A, B](ma: Kernel[A], f: A => B, g: B => A): Kernel[B] =
          ma.xmap(f, g)
#-scalaz
#+cats
        def imap[A, B](ma: Kernel[A])(f: A => B)(g: B => A): Kernel[B] =
          ma.imap(f)(g)
#-cats
      }

  }

}
