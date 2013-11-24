package doobie
package world

import doobie.util.RWSFWorld

trait IndexedWorld[R0] extends RWSFWorld[R0, Log, Int] {

  protected def next[A](f: (R, S) => A): Action[A] =
    for {
      s <- get
      r <- ask
      _ <- mod(_ + 1)
    } yield f(r, s)

}

