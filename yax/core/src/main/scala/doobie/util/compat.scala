package doobie.util

object compat {

#+cats
  object cats {
    import _root_.cats.{ Applicative, FlatMap, Monad, MonadCombine }
    import _root_.cats.std.list._

    object applicative {

      // these should appear in cats 0.7
      implicit class MoreCatsApplicativeOps[F[_], A](fa: F[A])(implicit A: Applicative[F]) {
        def replicateA(n: Int): F[List[A]] = A.sequence(List.fill(n)(fa))
        def unlessA(cond: Boolean): F[Unit] = if (cond) A.pure(()) else A.void(fa)
        def whenA(cond: Boolean): F[Unit] = if (cond) A.void(fa) else A.pure(())
      }

    }

    object monad {

      // these should appear in cats 0.7
      implicit class MoreCatsMonadOps[F[_], A](fa: F[A])(implicit M: Monad[F])
        extends applicative.MoreCatsApplicativeOps(fa)(M) {

        def whileM[G[_]](p: F[Boolean])(implicit G: MonadCombine[G]): F[G[A]] =
          M.ifM(p)(M.flatMap(fa)(x => M.map(whileM(p)(G))(xs => G.combineK(G.pure(x), xs))), M.pure(G.empty))

        def whileM_(p: F[Boolean]): F[Unit] =
          M.ifM(p)(M.flatMap(fa)(_ => whileM_(p)), M.pure(()))

        def untilM[G[_]](cond: F[Boolean])(implicit G: MonadCombine[G]): F[G[A]] =
          M.flatMap(fa)(x => M.map(whileM(M.map(cond)(!_))(G))(xs => G.combineK(G.pure(x), xs)))

        def untilM_(cond: F[Boolean]): F[Unit] =
          M.flatMap(fa)(_ => whileM_(M.map(cond)(!_)))

        def iterateWhile(p: A => Boolean): F[A] =
          M.flatMap(fa)(y => if (p(y)) iterateWhile(p) else M.pure(y))

        def iterateUntil(p: A => Boolean): F[A] =
          M.flatMap(fa)(y => if (p(y)) M.pure(y) else iterateUntil(p))
      }

    }

#+fs2
    object fs2 {
      import _root_.cats.data.Kleisli
      import _root_.fs2.util.{ Attempt, Effect }
      import _root_.fs2.interop.cats._

      implicit def catsKleisliFs2Effect[M[_], E](implicit c: Effect[M]): Effect[Kleisli[M, E, ?]] =
        new Effect[Kleisli[M, E, ?]] {
          def pure[A](a: A): Kleisli[M, E, A] = Kleisli.pure[M, E, A](a)
          def flatMap[A, B](ma: Kleisli[M, E, A])(f: A => Kleisli[M, E, B]): Kleisli[M, E, B] = ma.flatMap(f)
          def attempt[A](ma: Kleisli[M, E, A]): Kleisli[M, E, Attempt[A]] =
            Kleisli(e => c.attempt(ma.run(e)))
          def fail[A](t: Throwable): Kleisli[M, E, A] =
            Kleisli(e => c.fail(t))
          def suspend[A](ma: => Kleisli[M, E, A]): Kleisli[M, E, A] =
            Kleisli.pure[M, E, Unit](()).flatMap(_ => ma)
          def unsafeRunAsync[A](ma: Kleisli[M, E, A])(cb: Attempt[A] => Unit): Unit = Predef.???
        }
    }
#-fs2
  }
#-cats

}
