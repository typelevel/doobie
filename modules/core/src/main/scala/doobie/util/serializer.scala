// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.annotation.tailrec
import cats.Eval

/**
 * Module defining a transparent structure for conversion to `List[Any]`
 * with stack-safe operations.
 */
object serializer {

  sealed trait Serializer[A] extends Function1[A, List[Any]] {
    final override def apply(a: A) = {
      import Serializer._

      type Item = (α, α => List[Any]) forSome { type α }

      // `acc` is the processed part of the object.
      // We build the list starting from its tail for performance,
      // meaning that for a tuple `(A, B)`, `B` gets processed first.
      @tailrec
      def worker(stack: List[Item], acc: List[Any]): List[Any] =
        stack match {
          case List() => acc
          case (x, Opaque(f)) :: rest =>
            worker(rest, f(x) ++ acc)
          case (x, Contramap(f, sy)) :: rest =>
            worker((f(x), sy) :: rest, acc)
          case (x, Contramap2(f, sy, sz)) :: rest =>
            val (y, z) = f(x)
            worker((z, sz) :: (y, sy) :: rest, acc)
          case (x, Suspend(f)) :: rest =>
            worker((x, f.value) :: rest, acc)
          // TODO: This is for binary compatibility. Remove in 0.6.0.
          case (x, f) :: rest =>
            worker(rest, f(x) ++ acc)
        }

      worker(List((a, this)), List())
    }
  }

  object Serializer {
    final case class Opaque[A](f: A => List[Any]) extends Serializer[A]
    final case class Contramap2[A, B, C](
      f: A => (B, C),
      // We allow arbitrary functions to preserve binary compatibility.
      // TODO: Restrict these to `Serializer` in 0.6.0.
      sb: B => List[Any],
      sc: C => List[Any]
    ) extends Serializer[A]
    // Just an optimization
    final case class Contramap[A, B](
      f: A => B,
      sb: B => List[Any]
    ) extends Serializer[A]
    // For working with lazy instances
    final case class Suspend[A](f: Eval[A => List[Any]]) extends Serializer[A]

    def later[A](f: => (A => List[Any])): Serializer[A] =
      Serializer.Suspend(Eval.later(f))
  }
}
