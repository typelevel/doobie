package doobie.util

#+scalaz
import scalaz.{ Applicative, Functor }
import scalaz.syntax.applicative._

import scalaz.stream.{ Process, Sink, Cause }
import scalaz.stream.Process.{ bracket, repeatEval, eval_ }
#-scalaz
#+cats
import cats.{ Applicative, Functor }
import cats.implicits._
#-cats
#+fs2
import fs2.{ Stream => Process, Sink }
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

}
