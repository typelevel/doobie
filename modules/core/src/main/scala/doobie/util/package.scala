// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie

import cats.ApplicativeError
import cats.implicits._

/** Collection of modules for typeclasses and other helpful bits. */
package object util {

  private[util] def void(a: Any*): Unit =
    (a, ())._2

  implicit class MoreEitherOps[L, R](e: Either[L, R]) {
    def liftOnError[F[_]](handler: L => F[Unit])(
      implicit ev: ApplicativeError[F, L]
    ): F[R] =
      e.fold(l => handler(l) *> ev.raiseError[R](l), _.pure[F])
  }

}
