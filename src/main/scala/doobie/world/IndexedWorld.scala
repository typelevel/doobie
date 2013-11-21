package doobie
package world

import scalaz.effect.IO

trait IndexedWorld[S] extends World {

  protected type Index = Int // positive
  protected type State = (Index, S)

  protected def next[A](f: (S, Int) => A): Action[A] =
    action { case (n, s) => ((n + 1, s), f(s, n)) }

  implicit class RunnableAction[A](a: Action[A]) {
    def unsafeRun(s: S): IO[A] =
      IO(runWorld((1, s), a)._2)
  }

}

