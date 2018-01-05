// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.syntax

import doobie.util.transactor.Transactor
import doobie.free.connection.ConnectionIO

import scala.Predef.=:=

import cats.Monad
import cats.effect.Sync
import fs2.Stream

class StreamOps[F[_], A](fa: Stream[F, A]) {
  def vector(implicit ev: Sync[F]): F[Vector[A]] = fa.compile.toVector
  def list(implicit ev: Sync[F]): F[List[A]] = fa.compile.toList
  def sink(f: A => F[Unit])(implicit ev: Sync[F]): F[Unit] = fa.evalMap(f).compile.drain
  def transact[M[_]: Monad](xa: Transactor[M])(implicit ev: Stream[F, A] =:= Stream[ConnectionIO, A]): Stream[M, A] = xa.transP.apply(fa)
}

trait ToStreamOps {
  implicit def toDoobieStreamOps[F[_]: Sync, A](fa: Stream[F, A]): StreamOps[F, A] =
    new StreamOps(fa)
}

object stream extends ToStreamOps
