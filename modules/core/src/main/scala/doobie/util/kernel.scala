// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import java.sql.{ ResultSet, PreparedStatement }

import cats.{ Semigroupal, Invariant => InvariantFunctor }
import shapeless._
import shapeless.labelled.{ field, FieldType }
import doobie.util.safefold.{ SafeFold => S }

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

    def imap[B](f: A => B)(g: B => A): Kernel[B] =
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

        val split: I => (h.I, t.I) = {
          case ha :: ta => (h.ai(ha), t.ai(ta))
        }

        val ia      = (i: I) => i
        val ai      = (a: I) => a
        val get     = (rs: ResultSet, n: Int)     =>   h.ia(h.get(rs, n)) ::            t.ia(t.get(rs, n + h.width))
        val set     = combineSetLike[I](split)(h.width, h.set, t.set)
        val update  = combineSetLike[I](split)(h.width, h.update, t.update)
        val setNull = combineSetNull(h.width, h.setNull, t.setNull)
        val width   = h.width + t.width
      }

    val ohnil: Kernel[Option[HNil]] =
      new Kernel[Option[HNil]] {
        type I      = Option[HNil]
        val ia      = (i: I) => i
        val ai      = (a: I) => a
        val get     = (_: ResultSet, _: Int) => Some(HNil)
        val set     = (_: PreparedStatement, _: Int, _: I) => ()
        val setNull = (_: PreparedStatement, _: Int) => ()
        val update  = (_: ResultSet, _: Int, _: Option[HNil]) => ()
        val width   = 0
      }

    def ohcons1[H, T <: HList](H: Kernel[Option[H]], T: Kernel[Option[T]]): Kernel[Option[H :: T]] =
      new Kernel[Option[H :: T]]{

        val split: I => (H.I, T.I) = {
          case Some(h :: t) => (H.ai(Some(h)), T.ai(Some(t)))
          case None         => (H.ai(None), T.ai(None))
        }

        type I      = Option[H :: T]
        val ia      = (i: I) => i
        val ai      = (a: I) => a
        val get     = (rs: ResultSet, n: Int) => H.ia(H.get(rs, n)).flatMap(h => T.ia(T.get(rs, n + H.width)).map(h :: _))
        val set     = combineSetLike[I](split)(H.width, H.set, T.set)
        val update  = combineSetLike[I](split)(H.width, H.update, T.update)
        val setNull = combineSetNull(H.width, H.setNull, T.setNull)
        val width   = H.width + T.width

      }

    def ohcons2[H, T <: HList](H: Kernel[Option[H]], T: Kernel[Option[T]]): Kernel[Option[Option[H] :: T]] =
      new Kernel[Option[Option[H] :: T]]{

        val split: I => (H.I, T.I) = {
          case Some(h :: t) => (H.ai(h), T.ai(Some(t)))
          case None         => (H.ai(None), T.ai(None))
        }

        type I      = Option[Option[H] :: T]
        val ia      = (i: I) => i
        val ai      = (a: I) => a
        val get     = (rs: ResultSet, n: Int) => T.ia(T.get(rs, n + H.width)).map(H.ia(H.get(rs, n)) :: _)
        val set     = combineSetLike[I](split)(H.width, H.set, T.set)
        val update  = combineSetLike[I](split)(H.width, H.update, T.update)
        val setNull = combineSetNull(H.width, H.setNull, T.setNull)
        val width   = H.width + T.width
      }


    def product[A, B](a: Kernel[A], b: Kernel[B]): Kernel[(A, B)] =
      new Kernel[(A, B)] {
        type I      = (A, B)

        val split: I => (a.I, b.I) = {
          case (aa, ba) => (a.ai(aa), b.ai(ba))
        }

        val ia      = (i: I) => i
        val ai      = (a: I) => a
        val get     = (rs: ResultSet, n: Int) => ( a.ia(a.get(rs, n)), b.ia(b.get(rs, n + a.width)) )
        val set     = combineSetLike[I](split)(a.width, a.set, b.set)
        val update  = combineSetLike[I](split)(a.width, a.update, b.update)
        val setNull = combineSetNull(a.width, a.setNull, b.setNull)
        val width   = a.width + b.width
      }

    def record[K <: Symbol, H, T <: HList](h: Kernel[H], t: Kernel[T]): Kernel[FieldType[K, H] :: T] =
      new Kernel[FieldType[K, H] :: T] {
        type I      = FieldType[K, H] :: T

        val split: I => (h.I, t.I) = {
          case ha :: ta => (h.ai(ha), t.ai(ta))
        }

        val ia      = (i: I) => i
        val ai      = (a: I) => a
        val get     = (rs: ResultSet, n: Int) => field[K](h.ia(h.get(rs, n))) :: t.ia(t.get(rs, n + h.width))
        val set     = combineSetLike[I](split)(h.width, h.set, t.set)
        val update  = combineSetLike[I](split)(h.width, h.update, t.update)
        val setNull = combineSetNull(h.width, h.setNull, t.setNull)
        val width   = h.width + t.width
      }

    // These patterns come up a lot, and they are terribly noisy.

    @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
    private class CombineSetLikePartiallyApplied[I](val dummy: Boolean = true) extends AnyVal {
      def apply[A, I1, I2](split: I => (I1, I2))(
        xWidth: Int,
        sx: (A, Int, I1) => Unit,
        sy: (A, Int, I2) => Unit
      ): (A, Int, I) => Unit =
        S.product(
          S.fromFunction3(sx),
          S.fromFunction3(sy)
        ).contramap[(A, Int, I)]{ case (a, n, i) =>
            val (i1, i2) = split(i)
            ((a, n, i1), (a, n + xWidth, i2))
        }.asEffectFunction3
    }

    private def combineSetLike[I] = new CombineSetLikePartiallyApplied[I]

    private def combineSetNull(
      xWidth: Int,
      sx: (PreparedStatement, Int) => Unit,
      sy: (PreparedStatement, Int) => Unit
    ): (PreparedStatement, Int) => Unit =
      S.product(
        S.fromFunction2(sx),
        S.fromFunction2(sy)
      ).contramap[(PreparedStatement, Int)] { case (ps, n) =>
          ((ps, n), (ps, n + xWidth))
      }.asEffectFunction2
  }

  trait KernelInstances {
    // TODO: Invariant and Semigroupal (cats)

    implicit val kernelInvariantFunctor: InvariantFunctor[Kernel] =
      new InvariantFunctor[Kernel] {
        def imap[A, B](ma: Kernel[A])(f: A => B)(g: B => A): Kernel[B] =
          ma.imap(f)(g)
      }

    implicit val kernelCarterisn: Semigroupal[Kernel] =
      new Semigroupal[Kernel] {
        def product[A, B](fa: Kernel[A], fb: Kernel[B]): Kernel[(A, B)] =
          Kernel.product(fa, fb)
      }

  }

}
