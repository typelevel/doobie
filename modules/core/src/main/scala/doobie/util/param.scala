// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.annotation.implicitNotFound

import shapeless.{ HNil, HList, :: }

/** Module defining the `Param` typeclass. */
object param {

  /**
   * Witness that the elements of an HList each have an applicable Put instance.
   */
  @implicitNotFound("""
Cannot construct a parameter vector of the following type:

  ${A}

Because one or more types therein (disregarding HNil) does not have a Put
instance in scope. Try them one by one in the REPL or in your code:

  scala> Put[Foo]

and find the one that has no instance, then construct one as needed. Refer to
Chapter 12 of the book of doobie for more information.
""")
  sealed trait Param[A <: HList] {
    def elems(a: A): List[Param.Elem]
  }

  object Param {

    sealed trait Elem
    object Elem {
      final case class Arg[A](a: A, p: Put[A]) extends Elem
      final case class Opt[A](a: Option[A], p: Put[A]) extends Elem
    }

    def apply[L <: HList](implicit ev: Param[L]): Param[L] = ev

    /** There is an empty `Param` for `HNil`. */
    implicit val hnil: Param[HNil] =
      new Param[HNil] {
        def elems(a: HNil) = Nil
      }

    implicit def hcons[H, T <: HList](
      implicit pa: Put[H],
               pt: Param[T]
    ): Param[H :: T] =
      new Param[H :: T] {
        def elems(a: H :: T) =
          Elem.Arg(a.head, pa) :: pt.elems(a.tail)
      }

    implicit def hconsOpt[H, T <: HList](
      implicit pa: Put[H],
               pt: Param[T]
    ): Param[Option[H] :: T] =
      new Param[Option[H] :: T] {
        def elems(a: Option[H] :: T) =
          Elem.Opt(a.head, pa) :: pt.elems(a.tail)
      }

  }

}
