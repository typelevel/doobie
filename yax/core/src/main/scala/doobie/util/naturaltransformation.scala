package doobie.util

#+scalaz
import scalaz.{ Kleisli, Free, Monad, ~> }
#-scalaz
#+cats
import cats.{ Monad, ~> }
import cats.data.Kleisli
import cats.free.Free
#-cats

object naturaltransformation {

  // Natural transformation by Kleisli application.
  case class ApplyKleisli[F[_], E](e: E) extends (Kleisli[F, E, ?] ~> F) {
    def apply[A](fa: Kleisli[F, E, A]) = fa.run(e)
  }

  // Lift a natural translation over an algebra to one over its free monad.
  case class LiftF[F[_], G[_]: Monad](nat: F ~> G) extends (Free[F, ?] ~> G) {
    def apply[A](fa: Free[F, A]) = fa.foldMap(nat)
  }

}
