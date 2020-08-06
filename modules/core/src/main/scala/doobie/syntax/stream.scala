// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.syntax

import doobie.util.compat.=:=
import doobie.util.transactor.Transactor
import doobie.free.connection.ConnectionIO
import cats.Monad
import cats.data.Kleisli
import cats.effect.Sync
import fs2.Stream

class StreamOps[F[_], A](fa: Stream[F, A]) {
  def transact[M[_]: Monad](xa: Transactor[M])(implicit ev: Stream[F, A] =:= Stream[ConnectionIO, A]): Stream[M, A] = xa.transP.apply(fa)
}
class KleisliStreamOps[A, B](fa: Stream[Kleisli[ConnectionIO, A, *], B]) {
  def transact[M[_]: Monad](xa: Transactor[M]): Stream[Kleisli[M, A, *], B] = xa.transPK[A].apply(fa)
}

trait ToStreamOps {
  implicit def toDoobieStreamOps[F[_]: Sync, A](fa: Stream[F, A]): StreamOps[F, A] =
    new StreamOps(fa)
  implicit def toDoobieKleisliStreamOps[A,  B](fa: Stream[Kleisli[ConnectionIO, A, *], B]): KleisliStreamOps[A, B] =
    new KleisliStreamOps(fa)
}

object stream extends ToStreamOps
