// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.syntax

import doobie.util.param.Param
import doobie.util.pos.Pos
import doobie.util.fragment.Fragment
import shapeless.ProductArgs

/**
 * String interpolator for SQL literals. An expression of the form `sql".. $a ... $b ..."` with
 * interpolated values of type `A` and `B` (which must have `[[Param]]` instances, derived
 * automatically from `Put`) yields a value of type `[[Fragment]]`.
 */
final class SqlInterpolator(private val sc: StringContext)(implicit pos: Pos) {

  private def mkFragment[A](a: A, token: Boolean)(implicit ev: Param[A]): Fragment = {
    val sql = sc.parts.mkString("", "?", if (token) " " else "")
    Fragment(sql, a, Some(pos))(ev.composite)
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

  /** Alternative name for the `fr0` interpolator. */
  final val sql: fr0.type = fr0

  /**
   * Interpolator for a statement fragment that can contain interpolated values. Unlike `fr` no
   * attempt is made to be helpful with respect to whitespace.
   */
  object fr0 extends ProductArgs {
    def applyProduct[A: Param](a: A): Fragment = mkFragment(a, false)
  }

}

trait ToSqlInterpolator {
  implicit def toSqlInterpolator(sc: StringContext)(implicit pos: Pos): SqlInterpolator =
    new SqlInterpolator(sc)(pos)
}

object string extends ToSqlInterpolator
