package doobie.syntax

import doobie.util.transactor.Transactor
import doobie.free.connection.ConnectionIO

import scala.Predef.=:=

import cats.Monad
import cats.implicits._
import fs2.interop.cats._
import fs2.{ Stream => Process }
import fs2.util.{ Catchable, Suspendable }

/** Syntax for `Process` operations defined in `util.process`. */
object process {

  implicit class ProcessOps[F[_]: Catchable: Suspendable, A](fa: Process[F, A]) {

    def vector: F[Vector[A]] =
      fa.runLog.map(_.toVector)

    def list: F[List[A]] =
      fa.runLog.map(_.toList)

    def sink(f: A => F[Unit]): F[Unit] =
      fa.to(doobie.util.process.sink(f)).run

    def transact[M[_]: Monad](xa: Transactor[M])(implicit ev: Process[F, A] =:= Process[ConnectionIO, A]): Process[M, A] =
      xa.transP.apply(fa)

  }

}
