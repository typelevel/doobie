// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.syntax

import cats.data.{EitherT, OptionT}
import cats.effect.Bracket
import cats.syntax.functor._
import doobie.HC
import doobie.free.connection.{AsyncConnectionIO, ConnectionIO}
import doobie.util.transactor.Transactor

class ConnectionIOOps[A](ma: ConnectionIO[A]) {
  def transact[M[_]](xa: Transactor[M])(implicit ev: Bracket[M, Throwable]): M[A] = xa.transB.apply(ma)
}

class OptionTConnectionIOOps[A](ma: OptionT[ConnectionIO, A]) {
  def transact[M[_]](xa: Transactor[M])(implicit ev: Bracket[M, Throwable]): OptionT[M, A] =
    OptionT(
      xa.transB.apply(ma.orElseF(HC.rollback.as(None)).value)
    )
}

class EitherTConnectionIOOps[E, A](ma: EitherT[ConnectionIO, E, A]) {
  def transact[M[_]](xa: Transactor[M])(implicit ev: Bracket[M, Throwable]): EitherT[M, E, A] =
    EitherT(
      xa.transB.apply(ma.leftSemiflatMap(HC.rollback.as(_)).value)
    )
}

trait ToConnectionIOOps {
  implicit def toConnectionIOOps[A](ma: ConnectionIO[A]): ConnectionIOOps[A] =
    new ConnectionIOOps(ma)

  implicit def toOptionTConnectionIOOps[A](ma: OptionT[ConnectionIO, A]): OptionTConnectionIOOps[A] =
    new OptionTConnectionIOOps(ma)

  implicit def toEitherTConnectionIOOps[E, A](ma: EitherT[ConnectionIO, E, A]): EitherTConnectionIOOps[E, A] =
    new EitherTConnectionIOOps(ma)
}

object connectionio extends ToConnectionIOOps
