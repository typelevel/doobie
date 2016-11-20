package doobie.syntax

import doobie.util.atom._
import doobie.util.composite._
import doobie.util.query._
import doobie.util.update._
import doobie.util.update._
import doobie.util.param._
import doobie.util.fragment._
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

    private def mkFragment[A](a: A, token: Boolean)(implicit ev: Param[A]): Fragment = {
      val ps  = sc.parts.toList
      val qs  = ev.placeholders.map(placeholders) :+ (if (token) " " else "")
      val sql = (ps, qs).zipped.map(_ + _).mkString
      Fragment(sql, a)(ev.composite)
    }

    /**
     * Interpolator for a statement fragment that can contain interpolated values. When inserted
     * into the final SQL statement this fragment will be followed by a space. This is normally
     * what you want, and it makes it easier to concatenate fragments because you don't need to
     * think about intervening whitespace. If you do not want this behavior, use `fr0`.
     */
    object fr extends ProductArgs {
      def applyProduct[A: Param](a: A): Fragment = mkFragment(a, true)
    }

    /** Alternative name for the `fr` interpolator. */
    final val sql: fr.type = fr

    /**
     * Interpolator for a statement fragment that can contain interpolated values. Unlike `fr` no
     * attempt is made to be helpful with respect to whitespace.
     */
    object fr0 extends ProductArgs {
      def applyProduct[A: Param](a: A): Fragment = mkFragment(a, false)
    }

  }

}
