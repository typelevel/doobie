package doobie.syntax

import doobie.util.atom._
import doobie.util.composite._
import doobie.util.query._
import doobie.util.update._
import doobie.util.update._
import doobie.util.concat.Concat
import doobie.util.meta.Meta
import doobie.enum.nullability.NullabilityKnown
import java.sql.ResultSet
import Predef._

import doobie.hi._

import scala.annotation.implicitNotFound

#+scalaz
import scalaz._, Scalaz._
#-scalaz
#+cats
import cats.Reducible
import cats.implicits._
#-cats

import shapeless._

/** Module defining the `sql` string interpolator. */
object string {

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

  // If we have two composites of HLists we can concatenate them.
  implicit class MoreCompositeOps[A <: HList](ca: Composite[A]) {
    def +[B <: HList](cb: Composite[B])(
      implicit co: Concat[A, B]
    ): Composite[co.Out] =
      new Composite[co.Out] {
        val length: Int = ca.length + cb.length
        val meta: List[(Meta[_], NullabilityKnown)] = ca.meta ++ cb.meta
        val set: (Int, co.Out) => PreparedStatementIO[Unit] = { (n, o) =>
          val (a, b) = co.unapply(o)
          ca.set(n, a) *> cb.set(n + ca.length, b)
        }
        val unsafeGet: (ResultSet, Int) => co.Out = { (rs, n) =>
          val a = ca.unsafeGet(rs, n)
          val b = cb.unsafeGet(rs, n + ca.length)
          co.apply(a, b)
        }
        val update: (Int, co.Out) => ResultSetIO[Unit] = { (n, o) =>
          val (a, b) = co.unapply(o)
          ca.update(n, a) *> cb.update(n + ca.length, b)
        }
      }
  }


  sealed trait Fragment { outer =>
    type A <: HList
    val A: Composite[A]
    val a: A
    val sql: String

    def +[B <: HList](fb: Fragment.Aux[B])(
      implicit co: Concat[A, B]
    ): Fragment.Aux[co.Out] =
      new Fragment {
        type A = co.Out
        val A = outer.A + fb.A
        val a = co.apply(outer.a, fb.a)
        val sql = outer.sql + " " + fb.sql
      }

    def query[B](implicit B: Composite[B]): Query0[B] =
      Query[A, B](sql, None)(A, B).toQuery0(a)

  }
  object Fragment {
    type Aux[A0 <: HList] = Fragment { type A = A0 }
  }

  /**
   * String interpolator for SQL literals. An expression of the form `sql".. $a ... $b ..."` with
   * interpolated values of type `A` and `B` (which must have `[[Param]]` instances, derived
   * automatically from `Meta` via `Atom`) yields a value of type `[[Fragment.Aux[A :: B :: HNil]]]`.
   */
  implicit class SqlInterpolator(private val sc: StringContext) {

    private val stackFrame = {
      import Predef._
      Thread.currentThread.getStackTrace.lift(3)
    }

    private def placeholders(n: Int): String =
      List.fill(n)("?").mkString(", ")

    /**
     * Arity-abstracted method accepting a sequence of values along with `[[Param]]`
     * witnesses, yielding a `[[Builder]]``[...]` parameterized over the product of the types of the
     * passed arguments. This method uses the `ProductArgs` macro from Shapeless and has no
     * meaningful internal structure.
     */
    object sql extends ProductArgs {
      def applyProduct[A0 <: HList](a0: A0)(implicit ev: Param[A0]): Fragment.Aux[A0] =
        new Fragment {
          type A = A0
          val A = ev.composite
          val a = a0
          val sql = (sc.parts.toList, (ev.placeholders.map(placeholders) ++ List(""))).zipped.map(_ + _).mkString
        }
    }

  }

}
