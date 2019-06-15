// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.syntax

import doobie.util.compat.=:=
import doobie.util.transactor.Transactor
import doobie.free.connection.ConnectionIO

import cats.Monad
import cats.effect.Sync
import fs2.Stream

class StreamOps[F[_], A](fa: Stream[F, A]) {
  def transact[M[_]: Monad](xa: Transactor[M])(implicit ev: Stream[F, A] =:= Stream[ConnectionIO, A]): Stream[M, A] = xa.transP.apply(fa)
}

trait ToStreamOps {
  implicit def toDoobieStreamOps[F[_]: Sync, A](fa: Stream[F, A]): StreamOps[F, A] =
    new StreamOps(fa)
}

object stream extends ToStreamOps
