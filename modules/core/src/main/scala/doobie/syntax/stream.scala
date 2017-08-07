package doobie.syntax

import doobie.util.transactor.Transactor
import doobie.free.connection.ConnectionIO

import scala.Predef.=:=

import cats.effect.{ Effect, Sync }
import cats.implicits._
import fs2.Stream

/** Syntax for `Stream` operations defined in `util.process`. */
object stream {

  implicit class StreamOps[F[_]: Sync, A](fa: Stream[F, A]) {

    def vector: F[Vector[A]] =
      fa.runLog.map(_.toVector)

    def list: F[List[A]] =
      fa.runLog.map(_.toList)

    def sink(f: A => F[Unit]): F[Unit] =
      fa.to(doobie.util.stream.sink(f)).run

    def transact[M[_]: Effect](xa: Transactor[M])(implicit ev: Stream[F, A] =:= Stream[ConnectionIO, A]): Stream[M, A] =
      xa.transP.apply(fa)

  }

}
