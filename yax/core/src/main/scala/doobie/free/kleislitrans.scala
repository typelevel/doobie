package doobie.free

#+scalaz
import scalaz.{ Catchable, Free => F, Kleisli, Monad, ~>, \/ }
#-scalaz
#+cats
import cats.~>
import cats.data.Kleisli
import cats.free.{ Free => F }
import fs2.interop.cats._
#-cats
#+fs2
import fs2.util.{ Catchable, Suspendable, Monad }
import scala.util.{ Either => \/ }
#-fs2

import doobie.util.capture._

object kleislitrans {

  /**
   * Typeclass for algebras that have an instance of our default Kleisli interpreter. This allows us
   * to do arbitrary lifting rather than hard-coding each case.
   */
  trait KleisliTrans[Op[_]] { mod =>

    /** The carrier type for this interpreter; J for JDBC type. */
    type J

    /** Free monad over the free functor of `Op`. */
    type OpIO[A] = F[Op, A]

    /**
     * Natural transformation from `Op` to `Kleisli` for the given `M`, consuming a `J`.
     * @group Algebra
     */
#+scalaz
    def interpK[M[_]: Monad: Catchable: Capture]: Op ~> Kleisli[M, J, ?]
#-scalaz
#+fs2
    def interpK[M[_]: Catchable: Suspendable]: Op ~> Kleisli[M, J, ?]
#-fs2

    /**
     * Natural transformation from `OpIO` to `Kleisli` for the given `M`, consuming a `J`.
     * @group Algebra
     */
#+scalaz
    def transK[M[_]: Monad: Catchable: Capture]: OpIO ~> Kleisli[M, J, ?] =
#-scalaz
#+fs2
    def transK[M[_]: Catchable: Suspendable]: OpIO ~> Kleisli[M, J, ?] =
#-fs2
      new (OpIO ~> Kleisli[M, J, ?]) {
        def apply[A](ma: OpIO[A]): Kleisli[M, J, A] =
          ma.foldMap[Kleisli[M, J, ?]](interpK[M])
      }

    /**
     * Natural transformation from `OpIO` to `M`, given a `J`.
     * @group Algebra
     */
#+scalaz
    def trans[M[_]: Monad: Catchable: Capture](c: J): OpIO ~> M =
#-scalaz
#+fs2
    def trans[M[_]: Catchable: Suspendable](c: J): OpIO ~> M =
#-fs2
     new (OpIO ~> M) {
       def apply[A](ma: OpIO[A]): M[A] =
         transK[M].apply(ma).run(c)
     }

  }

  object KleisliTrans {
    type Aux[O[_], J0] = KleisliTrans[O] { type J = J0 }
  }

}
