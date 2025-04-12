// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.deriving.Mirror
import scala.compiletime.{erasedValue, summonAll}
import scala.compiletime

trait ReadPlatform extends LowestPriorityRead:
  trait Auto {
    implicit final inline def autoDerivedRead[A <: Product](using
        mirror: Mirror.ProductOf[A]
    ): Derived[Read[A]] =
      Derived(derivedImpl[A])
  }

  inline def derived[A <: Product](using
      mirror: Mirror.ProductOf[A]
  ): Read[A] =
    derivedImpl[A]

  private inline def derivedImpl[A <: Product](using
      mirror: Mirror.ProductOf[A]
  ): Read[A] =
    inline erasedValue[mirror.MirroredElemTypes] match {
      case _: EmptyTuple => compiletime.error(
          "Cannot derive Read instance for case objects or empty case classes (as they do not have any fields)")
      case _ =>
        val readsTuple: Tuple.Map[mirror.MirroredElemTypes, Read] =
          summonAll[Tuple.Map[mirror.MirroredElemTypes, Read]]
        Read.CompositeOfInstances[Any](readsTuple.productIterator.asInstanceOf[Iterator[Read[Any]]].toArray).map(a =>
          mirror.fromProduct(Tuple.fromArray(a)))
    }

  given tupleBase[H](
      using H: Read[H]
  ): Read[H *: EmptyTuple] =
    H.map(h => h *: EmptyTuple)

  given tuple[H, T <: Tuple](
      using
      H: Read[H],
      T: Read[T]
  ): Read[H *: T] =
    Read.Composite(H, T, (h, t) => h *: t)

  given optionalFromRead[A](using read: Read[A]): Read[Option[A]] = read.toOpt

  given fromGet[A](using get: Get[A]): Read[A] = new Read.Single(get)

trait LowestPriorityRead {
  given fromDerived[A](using ev: Derived[Read[A]]): Read[A] = ev.instance
}
