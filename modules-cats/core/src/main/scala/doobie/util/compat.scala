package doobie.util

object compat {

  object cats {
    import _root_.cats.{ Applicative, FlatMap, Monad, MonadCombine }
    import _root_.cats.instances.list._

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
  }

}
