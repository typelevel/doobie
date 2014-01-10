package doobie
package util

import scalaz._
import scalaz.syntax.id._
import scalaz.Id._
import scalaz.effect.IO
import scalaz.effect.MonadCatchIO

trait TWorld[State] {

  // Basically StateT with S sliced off
  case class ActionT[F[+_], +A](private[TWorld] run: State => F[(State, A)]) {
    def map[C](f: A => C)(implicit F: Functor[F]): ActionT[F, C] =
      new ActionT(s => F.map(run(s))(p => (p._1, f(p._2))))
    def flatMap[C](f: A => ActionT[F, C])(implicit M: Bind[F]): ActionT[F, C] =
      new ActionT(s => M.bind(run(s))(p => f(p._2).run(p._1)))
  }

  object ActionT extends X {
    implicit def monadActionT[M[+_]](implicit M: Monad[M]) =
      new Monad[({ type λ[+α] = ActionT[M, α] })#λ] {
        def point[A](a: => A): ActionT[M, A] = new ActionT(s => M.point((s, a)))
        override def map[A, B](fa: ActionT[M, A])(f: (A) => B): ActionT[M, B] = fa map f
        def bind[A, B](fa: ActionT[M, A])(f: (A) => ActionT[M, B]): ActionT[M, B] = fa flatMap f
      }
  }

  trait X { this: ActionT.type =>
    implicit object MonadIOActionT extends MonadCatchIO[({ type λ[+α] = ActionT[IO, α] })#λ] {
      def point[A](a: => A): ActionT[IO, A] = monadActionT[IO].point(a)
      def bind[A, B](fa: ActionT[IO, A])(f: A => ActionT[IO, B]): ActionT[IO, B] = fa.flatMap(f)
      def liftIO[A](ioa: IO[A]): ActionT[IO, A] = new ActionT(s => ioa.map((s, _)))
      def except[A](ma: ActionT[IO,A])(handler: Throwable => ActionT[IO,A]): ActionT[IO,A] =
        ActionT[IO,A](s => ma.run(s).except(handler(_).run(s))) // neato
    }
  }

  // ActionT with F sliced off
  class Lifted[F[+_]](implicit F: Applicative[F]) {
    type Action[+A] = ActionT[F, A]
    def action[A](f: State => (State, A)): Action[A] = ActionT(s => F.point(f(s)))
    def effect[A](f: State => A): Action[A] = action(s => (s, f(s)))
    def unit[A](a: => A): Action[A] = action(s => (s, a))
    final def eval[A](a: ActionT[F, A], s: State) = a.run(s)

  }

  // // This is what we're really after
  // protected val io = new Lifted[IO]
  // protected type Action[+A] = io.Action[A]
  // protected final def effect[A](f: State => A): Action[A] = io.effect(f)
  // protected final def eval[A](a: Action[A], s: State) = io.eval(a, s)

}






