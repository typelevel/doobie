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
  def applyKleisli[F[_], E](e: E) =
    λ[Kleisli[F, E, ?] ~> F](_.run(e))

  // Lift a natural translation over an functor to one over its free monad.
  def liftF[F[_], G[_]: Monad](nat: F ~> G) =
    λ[Free[F, ?] ~> G](_.foldMap(nat))

}
