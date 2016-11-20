package doobie.util

import doobie.free.preparedstatement.PreparedStatementIO
import doobie.util.atom.Atom
import doobie.util.composite.Composite

import scala.annotation.implicitNotFound

#+scalaz
import scalaz.Foldable1
import scalaz.syntax.foldable1._
import scalaz.syntax.applicative._
#-scalaz
#+cats
import cats._, cats.implicits._
#-cats

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
  sealed trait Param[A] {
    val composite: Composite[A]
    val placeholders: List[Int]
  }

  /**
   * Derivations for `Param`, which disallow embedding. Each interpolated query argument corresponds
   * with either an `Atom`, or with a singleton instance for a `NonEmptyList` of some atomic type,
   * derived with the `many` constructor.
   */
  object Param {

    def apply[A](implicit ev: Param[A]): Param[A] = ev

    /** Each `Atom` gives rise to a `Param`. */
    implicit def fromAtom[A](implicit ev: Atom[A]): Param[A] =
      new Param[A] {
        val composite = Composite.fromAtom(ev)
        val placeholders = List(1)
      }

    /** There is an empty `Param` for `HNil`. */
    implicit val ParamHNil: Param[HNil] =
      new Param[HNil] {
        val composite = Composite.emptyProduct
        val placeholders = Nil
      }

    /** Inductively we can cons a new `Param` onto the head of a `Param` of an `HList`. */
    implicit def ParamHList[H, T <: HList](implicit ph: Param[H], pt: Param[T]): Param[H :: T] =
      new Param[H :: T] {
        val composite = Composite.product[H,T](ph.composite, pt.composite)
        val placeholders = ph.placeholders ++ pt.placeholders
      }

#+scalaz
    /** A `Param` for a *singleton* `Foldable1`, used exclusively to support `IN` clauses. */
    def many[F[_] <: AnyRef : Foldable1, A](t: F[A])(implicit ev: Atom[A]): Param[t.type] =
#-scalaz
#+cats
    /** A `Param` for a *singleton* `Reducible`, used exclusively to support `IN` clauses. */
    def many[F[_] <: AnyRef : Reducible, A](t: F[A])(implicit ev: Atom[A]): Param[t.type] =
#-cats
      new Param[t.type] {
        val composite = new Composite[t.type] {
#+scalaz
          val length    = t.count
#-scalaz
#+cats
          val length    = t.foldMap(_ => 1)
#-cats
          val set       = (n: Int, in: t.type) =>
            t.foldLeft((n, ().pure[PreparedStatementIO])) { case ((n, psio), a) =>
              (n + 1, psio *> ev.set(n, a))
            } ._2
          val meta      = List.fill(length)(ev.meta)
          val unsafeGet = (_: java.sql.ResultSet, _: Int) => fail
          val update    = (_: Int, _: t.type) => fail
          def fail      = sys.error("singleton `IN` composite does not support get or update")
        }
#+scalaz
          val placeholders = List(t.count)
#-scalaz
#+cats
          val placeholders = List(t.foldMap(_ => 1))
#-cats
    }

  }

}
