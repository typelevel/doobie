package doobie.util

import scalaz.{ Applicative, Functor }
import scalaz.syntax.applicative._

import scalaz.stream.{ Process, Sink, Cause }
import scalaz.stream.Process.{ bracket, repeatEval, eval_, eval, halt, emitAll }

/** Additional functions for manipulating `Process` values. */
object process {

  /** Generalized `sink` constructor. */
  def sink[F[_]: Applicative, A](f: A => F[Unit]): Sink[F, A] =
    Process.repeatEval(f.point[F])

  /** Stream constructor for effectful source of chunks. */
  def repeatEvalChunks[F[_], T](fa: F[Seq[T]]): Process[F, T] = 
    eval(fa) flatMap { s =>
      if (s.isEmpty) halt
      else emitAll(s) ++ repeatEvalChunks(fa)
    }

}
