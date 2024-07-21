// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.*, cats.implicits.*

/** Module of additional functions for `Foldable`. */
object foldable {

  /** Like `foldSmash` but returns monoidal zero if the foldable is empty. */
  def foldSmash1[F[_]: Foldable, A](fa: F[A])(prefix: A, delim: A, suffix: A)(implicit A: Monoid[A]): A =
    if (fa.isEmpty) A.empty else fa.foldSmash(prefix, delim, suffix)

}
