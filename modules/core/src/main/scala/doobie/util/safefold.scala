// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.annotation.tailrec
import cats.{
  Eval,
  Monoid
}

/**
 * Module defining a structure for traversing data with stack-safe operations.
 */
object safefold {

  /**
    * A structure for building data traversal functions. This is similar to
    * `A => List[R]`, but has the added benefit of stack safety.
    */
  sealed trait SafeFold[A, R] {
    import SafeFold._

    final def combineAll(a: A)(implicit R: Monoid[R]): R =
      this.foldLeft(R.empty)(R.combine)(a)

    final def foldLeft[Z](z: Z)(step: (Z, R) => Z)(a: A): Z = {

      type Item = (α, SafeFold[α, R]) forSome { type α }

      // `acc` is the processed part of the object.
      @tailrec
      def worker(stack: List[Item], acc: Z): Z =
        stack match {
          case Nil => acc
          case (h :: rest) => h match {
            case (_, Empty()) =>
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

      worker(List((a, this)), z)
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
    private[util] final def asFunction(implicit R: Monoid[R]): A => R =
      AsFunction1(this)
  }

  object SafeFold {

    // Constructors

    // This is required to prevent capturing `Monoid` for `unit`.
    private final case class Empty[A, R]() extends SafeFold[A, R]

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

    private final case class AsFunction1[A, R: Monoid](
      asSafeFold: SafeFold[A, R]
    ) extends (A => R) {
      def apply(a: A): R = asSafeFold.combineAll(a)
    }

    /**
      * Get a [[SafeFold]], corresponding to given function:
      * - For functions built using [[SafeFold.asFunction]], returns the
      * original [[SafeFold]], preserving stack safety.
      * - For other functions returns a new opaque [[SafeFold]].
      */
    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    private[util] def fromFunction[A, R](f: A => R): SafeFold[A, R] =
      f match {
        // We're playing fast and loose with covariance here. We know that
        // `SafeFold` is covariant in `R` and contravariant in `A`, but proving
        // that through types brings more noise than feasible for this temporary
        // hack, as does using `narrow` and `widen`
        case AsFunction1(s) => s.asInstanceOf[SafeFold[A, R]]
        case _ => Opaque(f)
      }

    def opaque[A, R](f: A => R): SafeFold[A, R] = Opaque(f)

    def suspend[A, R](f: => SafeFold[A, R]): SafeFold[A, R] =
      SafeFold.Suspend(Eval.later(f))

    def trivial[A, R]: SafeFold[A, R] = Empty()

    def product[A, B, R](
      fa: SafeFold[A, R],
      fb: SafeFold[B, R]
    ): SafeFold[(A, B), R] =
      Product(fa, fb)
  }
}
