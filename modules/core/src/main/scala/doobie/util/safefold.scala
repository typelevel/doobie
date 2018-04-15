// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.annotation.tailrec
import cats.{
  Eval,
  Monoid
}
import cats.instances.unit._
import cats.evidence.===

/**
 * Module defining a structure for traversing data with stack-safe operations.
 */
object safefold {

  /**
    * A structure for building data traversal functions. This is similar to
    * `A => List[R]`, but has the added benefit of stack safety.
    */
  sealed trait SafeFold[A, R] { self =>
    import SafeFold._

    final def apply(a0: A): Applied[R] = new Applied[R] {
      type T = A
      val t = a0
      val st = self
    }

    final def product[B](fb: SafeFold[B, R]): SafeFold[(A, B), R] =
      SafeFold.product(this, fb)

    final def contramap[B](f: B => A): SafeFold[B, R] = Contramap(f, this)

    // TODO: this is a hack to preserve binary compatibility. Remove in 0.6.0.
    /**
      * Produce a function that uses this [[SafeFold]] as implementation.
      * The resulting function can be unwrapped using [[SafeFold.apply]].
      *
      * WARNING: The captured [[Monoid]] instance is lost when unwrapping, which
      * can lead to change in behaviour for overlapping instances.
      */
    private[util] final def asFunction1(implicit R: Monoid[R]): A => R =
      AsFunction1(this)

    private[util] final def asEffectFunction1(implicit ev: R === Unit): A => Unit =
      AsEffectFunction1(ev.substitute(this))
  }

  object SafeFold {

    // Constructors

    // This is required to prevent capturing `Monoid` for `unit`.
    private[util] final case class Trivial[A, R]() extends SafeFold[A, R]

    private final case class Opaque[A, R](
      f: A => R
    ) extends SafeFold[A, R]

    private final case class Product[A, B, R](
      sa: SafeFold[A, R],
      sb: SafeFold[B, R]
    ) extends SafeFold[(A, B), R]

    // Just an optimization
    private final case class Contramap[A, B, R](
      f: A => B,
      sb: SafeFold[B, R]
    ) extends SafeFold[A, R]

    // For working with lazy instances
    private final case class Suspend[A, R](
      f: Eval[SafeFold[A, R]]
    ) extends SafeFold[A, R]

    // Support for the binary compatibility hack. Remove in 0.6.0.
    // Make sure any combinators used in this library are overriden in such a way
    // that recovering the original `SafeFold` is possible.
    private final case class AsFunction1[A, R: Monoid](
      asSafeFold: SafeFold[A, R]
    ) extends (A => R) {
      def apply(a: A): R = asSafeFold(a).combineAll
    }

    private final case class AsEffectFunction1[A](
      asSafeFold: SafeFold[A, Unit]
    ) extends (A => Unit) {
      def apply(a: A): Unit = asSafeFold(a).combineAll
    }

    private final case class AsEffectFunction2[A, B](
      asSafeFold: SafeFold[(A, B), Unit]
    ) extends ((A, B) => Unit) {
      def apply(a: A, b: B): Unit = asSafeFold((a, b)).combineAll
      override def tupled = AsEffectFunction1(asSafeFold)
    }

    private final case class AsEffectFunction3[A, B, C](
      asSafeFold: SafeFold[(A, B, C), Unit]
    ) extends ((A, B, C) => Unit) {
      def apply(a: A, b: B, c: C): Unit = asSafeFold((a, b, c)).combineAll
      override def tupled = AsEffectFunction1(asSafeFold)
    }

    private[util] implicit class EffectOps2[A, B](val self: SafeFold[(A, B), Unit]) extends AnyVal {
      def asEffectFunction2: (A, B) => Unit =
        AsEffectFunction2(self)
    }

    private[util] implicit class EffectOps3[A, B, C](val self: SafeFold[(A, B, C), Unit]) extends AnyVal {
      def asEffectFunction3: (A, B, C) => Unit =
        AsEffectFunction3(self)
    }

    sealed trait Applied[R] {

      protected type T
      protected val t: T
      protected val st: SafeFold[T, R]

      final def combineAll(implicit R: Monoid[R]): R =
        foldLeft(R.empty)(R.combine)

      final def foldLeft[Z](z: Z)(step: (Z, R) => Z): Z = {

        type Item = (α, SafeFold[α, R]) forSome { type α }

        @tailrec
        def worker(stack: List[Item], acc: Z): Z =
          stack match {
            case Nil => acc
            case (h :: rest) => h match {
              case (_, Trivial()) =>
                worker(rest, acc)
              case (x, Opaque(f)) =>
                worker(rest, step(acc, f(x)))
              case (x, Contramap(f, sy)) =>
                worker((f(x), sy) :: rest, acc)
              case (x, Product(sy, sz)) =>
                val (y, z) = x
                worker((y, sy) :: (z, sz) :: rest, acc)
              case (x, Suspend(f)) =>
                worker((x, f.value) :: rest, acc)
            }
          }
        worker(List((t, st)), z)
      }
    }

    def opaque[A, R](f: A => R): SafeFold[A, R] = Opaque(f)

    def suspend[A, R](f: => SafeFold[A, R]): SafeFold[A, R] =
      SafeFold.Suspend(Eval.later(f))

    def trivial[A, R]: SafeFold[A, R] = Trivial()

    def product[A, B, R](
      fa: SafeFold[A, R],
      fb: SafeFold[B, R]
    ): SafeFold[(A, B), R] =
      Product(fa, fb)

    /**
      * Get a [[SafeFold]], corresponding to given function:
      * - For functions built using [[SafeFold.asFunction]], returns the
      * original [[SafeFold]], preserving stack safety.
      * - For other functions returns a new opaque [[SafeFold]].
      */
    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    private[util] def fromFunction1[A, R](f: A => R): SafeFold[A, R] =
      f match {
        // We're playing fast and loose with covariance here. We know that
        // `SafeFold` is covariant in `R` and contravariant in `A`, but proving
        // that through types brings more noise than feasible for this temporary
        // hack, as does using `narrow` and `widen`
        case AsFunction1(s) => s.asInstanceOf[SafeFold[A, R]]
        case AsEffectFunction1(s) => s.asInstanceOf[SafeFold[A, R]]
        case _ => Opaque(f)
      }

    private[util] def fromFunction2[A, B, R](f: (A, B) => R): SafeFold[(A, B), R] =
      fromFunction1(f.tupled)

    private[util] def fromFunction3[A, B, C, R](f: (A, B, C) => R): SafeFold[(A, B, C), R] =
      fromFunction1(f.tupled)
  }
}
