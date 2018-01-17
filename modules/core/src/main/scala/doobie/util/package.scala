// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie

import cats._, cats.data._, cats.implicits._

/**
 * Collection of modules for typeclasses and other helpful bits.
 */
package object util {

  private[util] implicit class MoreFoldableOps[F[_], A: Eq](fa: F[A])(implicit f: Foldable[F]) {
    def element(a: A): Boolean =
      f.exists(fa)(_ === a)
  }

  private[util] implicit class NelOps[A](as: NonEmptyList[A]) {
    def list: List[A] = as.head :: as.tail
  }

  private[util] def void(a: Any*): Unit = (a, ())._2

}
