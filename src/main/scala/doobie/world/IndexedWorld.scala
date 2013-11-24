package doobie
package world

import scalaz.effect.IO

trait IndexedWorld extends ReaderWriterStateFailWorld {

  protected type Index = Int // positive

  type S = Index

  protected def next[A](f: (R, S) => A): Action[A] =
    for {
      s <- get
      r <- ask
      _ <- mod(_ + 1)
    } yield f(r, s)
    // action { case (n, s) => ((n + 1, s), f(s, n)) }

}

