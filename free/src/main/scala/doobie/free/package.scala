package doobie

import scalaz._

package object free extends scalaz.syntax.ToCatchableOps {


  /** Interpret a free monad over a free functor of `S` via natural transformation to monad `M`. */
  def runFC[S[_], M[_], A](sa: Free.FreeC[S, A])(interp: S ~> M)(implicit M: Monad[M]): M[A] =
    sa.foldMap[M](new (({type λ[α] = Coyoneda[S, α]})#λ ~> M) {
      def apply[A](cy: Coyoneda[S, A]): M[A] =
        M.map(interp(cy.fi))(cy.k)
      })

  /** Universally quantified nonstrict lifting function. */
  type LiftM[M[_]] = scalaz.Forall[({type l[a] = (=> a) => M[a]})#l] 

  implicit val taskLiftM = {
    import scalaz.concurrent.Task
    new LiftM[Task] {
      def apply[A] = Task.delay 
    }
  }

  implicit val ioLiftM = {
    import scalaz.effect.IO
    new LiftM[IO] {
      def apply[A] = IO.apply 
    }
  }

  implicit val futureLiftM = {
    import scalaz.std.scalaFuture._
    import scala.concurrent.Future
    import scala.concurrent.ExecutionContext.Implicits.global
    new LiftM[Future] {
      def apply[A] = Future.apply
    }
  }

}

