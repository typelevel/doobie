package doobie.syntax

import doobie.util.transactor.Transactor
import doobie.util.capture.Capture
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
#+fs2
import fs2.{ Stream => Process }
import fs2.util.Catchable
import fs2.interop.cats.reverse._
#-fs2
#-cats

/** Syntax for `Process` operations defined in `util.process`. */
object process {

  implicit class ProcessOps[F[_]: Monad: Catchable: Capture, A](fa: Process[F, A]) {

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
