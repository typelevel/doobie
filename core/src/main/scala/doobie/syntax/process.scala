package doobie.syntax

import doobie.util.transactor.Transactor
import doobie.free.connection.ConnectionIO

import scala.Predef.=:=

import scalaz.{ Catchable, Monad }
import scalaz.stream.Process
import scalaz.syntax.monad._

/** Syntax for `Process` operations defined in `util.process`. */
object process {

  implicit class ProcessOps[F[_]: Monad: Catchable, A](fa: Process[F, A]) {

    def vector: F[Vector[A]] =
      fa.runLog.map(_.toVector)

    def list: F[List[A]] =
      fa.runLog.map(_.toList)

    def sink(f: A => F[Unit]): F[Unit] =     
      fa.to(doobie.util.process.sink(f)).run

    def transact[M[_]](xa: Transactor[M])(implicit ev: Process[F, A] =:= Process[ConnectionIO, A]): Process[M, A] =
      xa.transP(fa)

  }

}