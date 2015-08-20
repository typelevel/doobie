package doobie.syntax

import doobie.util.atom._
import doobie.util.composite._
import doobie.util.query._
import doobie.util.update._
import doobie.syntax.process._

import doobie.hi._

import scalaz.Monad
import scalaz.syntax.monad._
import scalaz.stream.Process

import shapeless._

/** Module defining the `sql` string interpolator. */
object string {

  /** 
   * String interpolator for SQL literals. An expression of the form `sql".. $a ... $b ..."` with
   * interpolated values of type `A` and `B` (which must have `[[doobie.util.atom.Atom Atom]]` 
   * instances) yields a value of type `[[Builder]]``[(A, B)]`.
   */
  implicit class SqlInterpolator(private val sc: StringContext) {

    private val stackFrame = {
      import Predef._
      Thread.currentThread.getStackTrace.lift(3)
    }

    /** 
     * Arity-abstracted method accepting a sequence of values along with `[[doobie.util.atom.Atom Atom]]` 
     * witnesses, yielding a `[[Builder]]``[...]` parameterized over the product of the types of the 
     * passed arguments. This method uses the `ProductArgs` macro from Shapeless and has no
     * meaningful internal structure.
     */
    object sql extends ProductArgs {
      def applyProduct[A: Composite](a: A): Builder[A] = 
        new Builder(a, sc.parts.mkString("?"), stackFrame)
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

    /** Construct an `[[doobie.util.update.Update0 Update0]]` from this `[[Builder]]`. */
    def update: Update0 =
      Update[A](rawSql, stackFrame).toUpdate0(a)

  }

}
