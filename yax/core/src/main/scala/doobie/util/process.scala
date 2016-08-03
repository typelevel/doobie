package doobie.util

#+scalaz
import scalaz.{ Applicative, Functor }
import scalaz.syntax.applicative._

import scalaz.stream.{ Process, Sink, Cause }
import scalaz.stream.Process.{ bracket, repeatEval, eval_, eval, halt, emitAll }
#-scalaz
#+cats
import cats.{ Applicative, Functor }
import cats.implicits._
#-cats
#+fs2
import fs2.{ Stream => Process, Sink }
import fs2.Stream.{ attemptEval, fail, emits, empty }
#-fs2

/** Additional functions for manipulating `Process` values. */
object process {

  /** Generalized `sink` constructor. */
  def sink[F[_]: Applicative, A](f: A => F[Unit]): Sink[F, A] =
#+scalaz
    Process.repeatEval(f.point[F])
#-scalaz
#+fs2
    _.flatMap(a => Process.eval(f(a)))
#-fs2

  /** Stream constructor for effectful source of chunks. */
  def repeatEvalChunks[F[_], T](fa: F[Seq[T]]): Process[F, T] = 
#+scalaz
    eval(fa) flatMap { s =>
      if (s.isEmpty) halt
      else emitAll(s) ++ repeatEvalChunks(fa)
    }
#-scalaz
#+fs2
    attemptEval(fa) flatMap {
      case Left(e)    => fail(e)
      case Right(seq) => if (seq.isEmpty) empty else (emits(seq) ++ repeatEvalChunks(fa))
    }
#-fs2

}
