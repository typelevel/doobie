package doobie.syntax

import doobie.util.atom._
import doobie.util.composite._
import doobie.util.query._
import doobie.util.update._
import doobie.util.log.LogHandler

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

  /**
   * String interpolator for SQL literals. An expression of the form `sql".. $a ... $b ..."` with
   * interpolated values of type `A` and `B` (which must have `[[Param]]` instances, derived
   * automatically from `Meta` via `Atom`) yields a value of type `[[Builder]]``[(A, B)]`.
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
      def applyProduct[A <: HList](a: A)(implicit ev: Param[A]) = { // scalastyle:ignore
        import Predef._
        val sql = (sc.parts.toList, (ev.placeholders.map(placeholders) ++ List(""))).zipped.map(_ + _).mkString
        new Builder(a, sql, stackFrame)(ev.composite)
      }
    }

  }

  /**
   * Type computed by the `sql` interpolator, parameterized over the composite of the types of
   * interpolated arguments. This type captures the sql string and parameter types, which can
   * subsequently transformed into a `[[doobie.util.query.Query0 Query0]]` or
   * `[[doobie.util.update.Update0 Update0]]` (see the associated methods).
   */
  final class Builder[A: Composite] private[string] (a: A, rawSql: String, stackFrame: Option[StackTraceElement]) {

    /**
     * Construct a `[[doobie.util.query.Query0 Query0]]` from this `[[Builder]]`, parameterized over a
     * composite output type.
     */
    def query[O: Composite]: Query0[O] =
      Query[A, O](rawSql, stackFrame).toQuery0(a)

    def queryL[O: Composite](h: LogHandler[A]): Query0[O] =
      Query[A, O](rawSql, stackFrame, Some(h)).toQuery0(a)

    /** Construct an `[[doobie.util.update.Update0 Update0]]` from this `[[Builder]]`. */
    def update: Update0 =
      Update[A](rawSql, stackFrame).toUpdate0(a)

    def updateL(h: LogHandler[A]): Update0 =
      Update[A](rawSql, stackFrame, Some(h)).toUpdate0(a)

  }

}
