// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.annotation.tailrec
import cats.{
  Eval,
  Monoid
}
import cats.syntax.monoid._

/**
 * Module defining a transparent structure for conversion to `List[Any]`
 * with stack-safe operations.
 */
object serializer {

  sealed trait Serializer[A, R] {
    import Serializer._

    final def combineAll(a: A)(implicit R: Monoid[R]): R = {

      type Item = (α, Serializer[α, R]) forSome { type α }

      // `acc` is the processed part of the object.
      // We build the object starting from the right for performance (think
      // building a list from its tail), meaning that for a tuple `(A, B)`,
      // `B` gets processed first.
      @tailrec
      def worker(stack: List[Item], acc: R): R =
        stack match {
          case Nil => acc
          case (h :: rest) => h match {
            case (_, Empty()) =>
              worker(rest, acc)
            case (x, Opaque(f)) =>
              worker(rest, f(x) |+| acc)
            case (x, Contramap(f, sy)) =>
              worker((f(x), sy) :: rest, acc)
            case (x, Product(sy, sz)) =>
              val (y, z) = x
              worker((z, sz) :: (y, sy) :: rest, acc)
            case (x, Suspend(f)) =>
              worker((x, f.value) :: rest, acc)
          }
        }

      worker(List((a, this)), Monoid.empty)
    }

    final def product[B](fb: Serializer[B, R]): Serializer[(A, B), R] =
      Serializer.product(this, fb)

    final def contramap[B](f: B => A): Serializer[B, R] = Contramap(f, this)

    // TODO: this is a hack to preserve binary compatibility. Remove in 0.6.0.
    /**
      * Produce a function that uses this [[Serializer]] as implementation.
      * The resulting function can be unwrapped using [[Serializer.apply]].
      *
      * WARNING: The captured [[Monoid]] instance is lost when unwrapping, which
      * can lead to change in behaviour for overlapping instances.
      */
    private[util] final def asFunction(implicit R: Monoid[R]): A => R =
      AsFunction1(this)
  }

  object Serializer {

    // Constructors

    // This is required to prevent capturing `Monoid` for `unit`.
    private final case class Empty[A, R]() extends Serializer[A, R]
    private final case class Opaque[A, R](
      f: A => R
    ) extends Serializer[A, R]
    private final case class Product[A, B, R](
      sa: Serializer[A, R],
      sb: Serializer[B, R]
    ) extends Serializer[(A, B), R]
    // Just an optimization
    private final case class Contramap[A, B, R](
      f: A => B,
      sb: Serializer[B, R]
    ) extends Serializer[A, R]
    // For working with lazy instances
    private final case class Suspend[A, R](
      f: Eval[Serializer[A, R]]
    ) extends Serializer[A, R]

    // Support for the binary compatibility hack. Remove in 0.6.0.

    private final case class AsFunction1[A, R: Monoid](
      asSerializer: Serializer[A, R]
    ) extends (A => R) {
      def apply(a: A): R = asSerializer.combineAll(a)
    }

    /**
      * Get a [[Serializer]], corresponding to given function:
      * - For functions built using [[Serializer.asFunction]], returns the
      * original [[Serializer]], preserving stack safety.
      * - For other functions returns a new opaque [[Serializer]].
      */
    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    private[util] def fromFunction[A, R](f: A => R): Serializer[A, R] =
      f match {
        // We're playing fast and loose with covariance here. We know that
        // `Serializer` is covariant in `R` and contravariant in `A`, but proving
        // that through types brings more noise than feasible for this temporary
        // hack, as does using `narrow` and `widen`
        case AsFunction1(s) => s.asInstanceOf[Serializer[A, R]]
        case _ => Opaque(f)
      }

    def opaque[A, R](f: A => R): Serializer[A, R] = Opaque(f)

    def suspend[A, R](f: => Serializer[A, R]): Serializer[A, R] =
      Serializer.Suspend(Eval.later(f))

    def trivial[A, R]: Serializer[A, R] = Empty()

    def product[A, B, R](
      fa: Serializer[A, R],
      fb: Serializer[B, R]
    ): Serializer[(A, B), R] =
      Product(fa, fb)
  }
}
