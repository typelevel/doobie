package doobie.util

import doobie.util.meta.Meta
import doobie.util.composite.Composite

import scala.annotation.implicitNotFound

import shapeless.{ HNil, HList, :: }

/** Module defining the `Param` typeclass. */
object param {

  /**
   * Typeclass for a flat vector of `Atom`s, analogous to `Composite` but with no nesting or
   * generalization to product types. Each element expands to some nonzero number of `?`
   * placeholders in the SQL literal, and the param vector itself has a `Composite` instance.
   */
  @implicitNotFound("""Could not find or construct Param[${A}].
Ensure that this type is an atomic type with an Atom instance in scope, or is an HList whose members
have Atom instances in scope. You can usually diagnose this problem by trying to summon the Atom
instance for each element in the REPL. See the FAQ in the Book of Doobie for more hints.""")
  final case class Param[A](composite: Composite[A])

  /**
   * Derivations for `Param`, which disallow embedding. Each interpolated query argument corresponds
   * with a type with a `Meta` instance, or an `Option` thereof.
   */
  object Param {

    def apply[A](implicit ev: Param[A]): Param[A] = ev

    /** Each `Meta[A]` gives rise to a `Param[A]`. */
    implicit def fromMeta[A](implicit ev: Meta[A]): Param[A] =
      Param[A](Composite.fromMeta(ev))

    /** Each `Meta[A]` gives rise to a `Param[Option[A]]`. */
    implicit def fromMetaOption[A](implicit ev: Meta[A]): Param[Option[A]] =
      Param[Option[A]](Composite.fromMetaOption(ev))

    /** There is an empty `Param` for `HNil`. */
    implicit val ParamHNil: Param[HNil] =
      Param[HNil](Composite.emptyProduct)

    /** Inductively we can cons a new `Param` onto the head of a `Param` of an `HList`. */
    implicit def ParamHList[H, T <: HList](implicit ph: Param[H], pt: Param[T]): Param[H :: T] =
      new Param[H :: T](Composite.product[H,T](ph.composite, pt.composite))

  }

}
