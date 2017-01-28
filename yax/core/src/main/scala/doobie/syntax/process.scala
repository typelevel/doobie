package doobie.syntax

import doobie.util.transactor.Transactor
#+scalaz
import doobie.util.capture.Capture
#-scalaz
import doobie.free.connection.ConnectionIO

import scala.Predef.=:=

#+scalaz
import scalaz.{ Catchable, DList, Monad }
import scalaz.stream.Process
import scalaz.syntax.monad._
#-scalaz
#+cats
import cats.Monad
import cats.implicits._
import fs2.interop.cats._
#-cats
#+fs2
import fs2.{ Stream => Process }
import fs2.util.{ Catchable, Suspendable }
#-fs2

/** Syntax for `Process` operations defined in `util.process`. */
object process {

#+scalaz
  implicit class ProcessOps[F[_]: Monad: Catchable: Capture, A](fa: Process[F, A]) {
#-scalaz
#+fs2
  implicit class ProcessOps[F[_]: Catchable: Suspendable, A](fa: Process[F, A]) {
#-fs2

    def vector: F[Vector[A]] =
      fa.runLog.map(_.toVector)

    def list: F[List[A]] =
      fa.runLog.map(_.toList)

    def sink(f: A => F[Unit]): F[Unit] =
      fa.to(doobie.util.process.sink(f)).run

    def transact[M[_]: Monad](xa: Transactor[M, _])(implicit ev: Process[F, A] =:= Process[ConnectionIO, A]): Process[M, A] =
      xa.transP.apply(fa)

  }

}
