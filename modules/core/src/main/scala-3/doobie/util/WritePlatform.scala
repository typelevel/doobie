// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.util

import scala.deriving.Mirror

trait WritePlatform extends LowestPriorityWrite:

  trait Auto {
    implicit final inline def autoDerivedWrite[A <: Product](using Mirror.ProductOf[A]): Derived[Write[A]] =
      ${ WriteMacros.autoDerived[A] }
  }

  /** Derives a Write for a case class. Each field must have a Write (or Put) instance in scope. */
  inline def derived[A <: Product](using Mirror.ProductOf[A]): Write[A] =
    ${ WriteMacros.derived[A] }

  given tupleBase[H](
      using H: Write[H]
  ): Write[H *: EmptyTuple] =
    Write.Composite[H *: EmptyTuple](
      List(H),
      {
        case h *: EmptyTuple => List(h)
      }
    )

  given tuple[H, T <: Tuple](
      using
      H: Write[H],
      T: Write[T]
  ): Write[H *: T] =
    Write.Composite(
      List(H, T),
      {
        case h *: t => List(h, t)
      }
    )

  given optionalFromWrite[A](using write: Write[A]): Write[Option[A]] =
    write.toOpt

  given fromPut[A](using put: Put[A]): Write[A] =
    new Write.Single(put)

trait LowestPriorityWrite {
  implicit def fromDerived[A](implicit ev: Derived[Write[A]]): Write[A] = ev.instance
}
