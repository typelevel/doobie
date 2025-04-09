// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import shapeless.labelled.FieldType
import shapeless.{Generic, HList, IsTuple, Lazy, OrElse}
import shapeless.{::, HNil}

import scala.annotation.implicitNotFound

trait ReadPlatform extends LowerPriority1Read {

  def derived[A](implicit
      @implicitNotFound(
        "Cannot derive Read instance. Please check that each field in the case class has a Read instance or can derive one")
      ev: Derived[MkRead[A]]
  ): Read[A] = ev.instance.underlying

  trait Auto extends MkReadInstances

  implicit def fromReadOption[A](implicit read: Read[A]): Read[Option[A]] = read.toOpt
}

trait LowerPriority1Read extends LowerPriority2Read {

  implicit def fromGet[A](implicit get: Get[A]): Read[A] = new Read.Single(get)

}

trait LowerPriority2Read extends LowerPriority3Read {

  // Derivation for product types (i.e. case class)
  implicit def genericTuple[A, Repr <: HList](implicit
      gen: Generic.Aux[A, Repr],
      G: Lazy[Read[Repr]],
      isTuple: IsTuple[A]
  ): Read[A] = {
    val _ = isTuple
    implicit val r: Lazy[Read[Repr] OrElse Derived[MkRead[Repr]]] = G.map(OrElse.primary(_))
    MkRead.genericRead[A, Repr].instance
  }

  @deprecated("Read.generic has been renamed to Read.derived to align with Scala 3 derivation", "1.0.0-RC6")
  def generic[T, Repr <: HList](
      implicit
      gen: Generic.Aux[T, Repr],
      G: Lazy[Read[Repr] OrElse Derived[MkRead[Repr]]]
  ): Read[T] =
    MkRead.genericRead[T, Repr].instance

  implicit def recordBase[K <: Symbol, H](
      implicit H: Read[H]
  ): Read[FieldType[K, H] :: HNil] = MkRead.recordBase[K, H].instance

  implicit def productBase[H](
      implicit H: Read[H]
  ): Read[H :: HNil] = MkRead.productBase[H].instance
}

trait LowerPriority3Read extends LowestPriorityRead {

  implicit def product[H, T <: HList](
      implicit
      H: Read[H],
      T: Read[T]
  ): Read[H :: T] = MkRead.product[H, T].instance

  implicit def record[K <: Symbol, H, T <: HList](
      implicit
      H: Read[H],
      T: Read[T]
  ): Read[FieldType[K, H] :: T] = MkRead.record[K, H, T].instance
}

trait LowestPriorityRead {
  implicit def fromDerived[A](implicit ev: Derived[Read[A]]): Read[A] = ev.instance
}
