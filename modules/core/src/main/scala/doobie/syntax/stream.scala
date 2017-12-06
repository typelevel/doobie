// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.syntax

import doobie.util.transactor.Transactor
import doobie.free.connection.ConnectionIO

import scala.Predef.=:=

import cats.effect.{ Effect, Sync }
import cats.implicits._
import fs2.Stream

class StreamOps[F[_]: Sync, A](fa: Stream[F, A]) {
  def vector: F[Vector[A]] = fa.runLog.map(_.toVector)
  def list: F[List[A]] = fa.runLog.map(_.toList)
  def sink(f: A => F[Unit]): F[Unit] = fa.evalMap(f).run
  def transact[M[_]: Effect](xa: Transactor[M])(implicit ev: Stream[F, A] =:= Stream[ConnectionIO, A]): Stream[M, A] = xa.transP.apply(fa)
}

trait ToStreamOps {
  implicit def toDoobieStreamOps[F[_]: Sync, A](fa: Stream[F, A]): StreamOps[F, A] =
    new StreamOps(fa)
}

object stream extends ToStreamOps
