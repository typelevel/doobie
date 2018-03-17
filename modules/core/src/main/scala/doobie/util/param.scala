// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.annotation.implicitNotFound

import shapeless.{ HNil, HList, :: }

/** Module defining the `Param` typeclass. */
object param {

  /**
   * Typeclass for a flat vector of `Put`s, analogous to `Write` but with no nesting or
   * generalization to product types. Each element expands to some nonzero number of `?`
   * placeholders in the SQL literal, and the param vector itself has a `Write` instance.
   */
  @implicitNotFound("""Could not find or construct Param[${A}].
Ensure that this type is an atomic type with an Put instance in scope, or is an HList whose members
have Put instances in scope. You can usually diagnose this problem by trying to summon the Put
instance for each element in the REPL. See the FAQ in the Book of Doobie for more hints.""")
  final class Param[A](val composite: Write[A])

  /**
   * Derivations for `Param`, which disallow embedding. Each interpolated query argument corresponds
   * with a type with a `Put` instance, or an `Option` thereof.
   */
  object Param {

    def apply[A](implicit ev: Param[A]): Param[A] = ev

    /** Each `Meta[A]` gives rise to a `Param[A]`. */
    implicit def fromMeta[A: Put]: Param[A] =
      new Param[A](Write.fromPut[A])

    /** Each `Meta[A]` gives rise to a `Param[Option[A]]`. */
    implicit def fromMetaOption[A: Put]: Param[Option[A]] =
      new Param[Option[A]](Write.fromPutOption[A])

    /** There is an empty `Param` for `HNil`. */
    implicit val ParamHNil: Param[HNil] =
      new Param[HNil](Write.emptyProduct)

    /** Inductively we can cons a new `Param` onto the head of a `Param` of an `HList`. */
    implicit def ParamHList[H, T <: HList](implicit ph: Param[H], pt: Param[T]): Param[H :: T] =
      new Param[H :: T](Write.product[H,T](ph.composite, pt.composite))

  }

}
