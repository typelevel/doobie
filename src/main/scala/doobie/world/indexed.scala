package doobie
package world

import scalaz._

trait IndexedWorld extends DWorld {

  protected type S = Int

  protected[world] def runi[A](r: R, a: Action[A]): (W, Throwable \/ A) =
    runrws(r, 1, a) match { case (w, _ , e) => (w, e) }

  protected def next[A](f: (R, Int) => A): Action[A] =
    for {
      s <- get
      r <- ask
      _ <- mod(_ + 1)
    } yield f(r, s)

}


