package doobie.free

import scalaz.{ Catchable, Coyoneda, Free, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._
import doobie.util.trace.Trace

object kleislitrans {

  /** 
   * Typeclass for algebras that have an instance of our default Kleisli interpreter. This allows us
   * to do arbitrary lifting rather than hard-coding each case.
   */
  trait KleisliTrans[F[_]] { mod =>

    /** The carrier type for this interpreter; J for JDBC type. */
    type J

    /** Free monad over the free functor of `F`. */
    type OpIO[A] = Free.FreeC[F, A]

    /** 
     * Natural transformation from `F` to `Kleisli` for the given `M`, consuming a `J`. 
     * @group Algebra
     */
    def interpK[M[_]: Monad: Catchable: Capture]: F ~> Kleisli[M, J, ?]

    /** 
     * Natural transformation from `F` to `Kleisli` for the given `M`, consuming a `(Trace[M], J)`. 
     * @group Algebra
     */
    def interpKL[M[_]: Monad: Catchable: Capture]: F ~> Kleisli[M, (Trace[M], J), ?]

    /** 
     * Natural transformation from `OpIO` to `Kleisli` for the given `M`, consuming a `J`. 
     * @group Algebra
     */
    def transK[M[_]: Monad: Catchable: Capture]: OpIO ~> Kleisli[M, J, ?] =
      new (OpIO ~> Kleisli[M, J, ?]) {
        def apply[A](ma: OpIO[A]): Kleisli[M, J, A] =
          Free.runFC[F, Kleisli[M, J, ?], A](ma)(interpK[M])
      }

    /** 
     * Natural transformation from `OpIO` to `Kleisli` for the given `M`, consuming a `(Trace[M], J)`. 
     * @group Algebra
     */
    def transKL[M[_]: Monad: Catchable: Capture]: OpIO ~> Kleisli[M, (Trace[M], J), ?] =
      new (OpIO ~> Kleisli[M, (Trace[M], J), ?]) {
        def apply[A](ma: OpIO[A]): Kleisli[M, (Trace[M], J), A] =
          Free.runFC[F, Kleisli[M, (Trace[M], J), ?], A](ma)(interpKL[M])
      }

    /** 
     * Natural transformation from `OpIO` to `M`, given a `J`. 
     * @group Algebra
     */
    def trans[M[_]: Monad: Catchable: Capture](c: J): OpIO ~> M =
     new (OpIO ~> M) {
       def apply[A](ma: OpIO[A]): M[A] = 
         transK[M].apply(ma).run(c)
     }

    /** 
     * Natural transformation from `OpIO` to `M`, given a `Trace[M]` and a `J`. 
     * @group Algebra
     */
    def transL[M[_]: Monad: Catchable: Capture](t: Trace[M], c: J): OpIO ~> M =
     new (OpIO ~> M) {
       def apply[A](ma: OpIO[A]): M[A] = 
         transKL[M].apply(ma).run((t, c))
     }

  }

  object KleisliTrans {
    type Aux[O[_], J0] = KleisliTrans[O] { type J = J0 }
  }

  implicit class KleisliTransOps[F[_], J, A](ma: Free.FreeC[F, A])(implicit kt: KleisliTrans.Aux[F, J]) {

    // ok this works, so here is how we get our syntax now
    def newTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, J, A] =
      kt.transK[M].apply(ma)

    // ok this works, so here is how we get our syntax now
    def newTransKL[M[_]: Monad: Catchable: Capture]: Kleisli[M, (Trace[M], J), A] =
      kt.transKL[M].apply(ma)

  }

}