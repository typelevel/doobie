package doobie.util

import cats._

object monaderror {

  implicit class MoreMonadErrorOps[F[_], E, A](fa: F[A])(implicit ev: MonadError[F, E]) {

    def guarantee(finalizer: F[Unit]): F[A] =
      ev.flatMap(ev.attempt(fa)) { e =>
        ev.flatMap(finalizer)(_ => e.fold(ev.raiseError, ev.pure))
      }

    def onError(handler: F[_]): F[A] =
      ev.handleErrorWith(fa)(e => ev.flatMap(handler)(_ => ev.raiseError(e)))

  }



}
