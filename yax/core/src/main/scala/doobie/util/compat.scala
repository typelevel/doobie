package doobie.util

object compat {

// #+fs2
//   object fs2 {
//     import _root_.fs2.util.{ Catchable => Fs2Catchable}

//     implicit def doobieCatchableToFs2Catchable[M[_]: Monad](implicit c: Catchable[M]): fs2.util.Catchable[M] =
//       new fs2.util.Catchable[M] {
//         def flatMap[A, B](a: M[A])(f: A => M[B]) = a.flatMap(f)
//         def pure[A](a: A) = a.pure[M]
//         def attempt[A](ma: M[A]) = c.attempt(ma).map(_.toEither)
//         def fail[A](t: Throwable) = c.fail(t)
//       }

//   }
// #-fs2

#+cats
  object cats {
    import _root_.cats.{ ~> => CatsNat }

#+fs2
    object fs2 {
      import _root_.fs2.util.{ ~> => Fs2Nat }

      implicit def naturalTransformationCompat[F[_], G[_]](nat: CatsNat[F, G]): Fs2Nat[F, G] =
        new Fs2Nat[F, G] {
          def apply[A](fa: F[A]) = nat(fa)
        }

    }
#-fs2

  }
#-cats

}