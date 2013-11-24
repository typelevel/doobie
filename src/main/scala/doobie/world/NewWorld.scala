package doobie
package world

import scala.annotation.tailrec
import scalaz._
import scalaz.Scalaz._
import scalaz.Free._
import scalaz.effect._
import language.higherKinds

/* Stateful effect world for computations that can fail. */
trait FailWorld {

  type State

  // Our algebra is trivial; all operations are state transitions.
  case class Op[+A](f: State => (State, Throwable \/ A))

  // A functor here gives us a Free monad.
  implicit val OpFunctor: Functor[Op] =  
    new Functor[Op] {
      def map[A, B](op: Op[A])(g: A => B) = 
        Op(op.f(_).map(_.rightMap(g)))
    }

  // Thus. 
  type Action[A] = Free[Op, A]
  Monad[Action] // proof

  ////// INTERPRETER

  // Each time we turn the crank we trap exceptions, both in resume (if map throws, for instance)
  // and in the implementation of each Op. This allows us to preserve and return the last known
  // good state along with the failure, which is pretty neat.
  @tailrec final def run[A](s: State, a: Action[A]): (State, Throwable \/ A) = 
    \/.fromTryCatch(a.resume) match { // N.B. we use tailrec with folds, so patterns it is
      case \/-(-\/(Op(f))) =>
        \/.fromTryCatch(f(s)) match {
          case \/-((s, \/-(a))) => run(s, a)
          case \/-((s, -\/(t))) => (s, t.left)
          case -\/(t) => (s, t.left)
        }
      case \/-(\/-(a)) => (s, a.right)
      case -\/(t)      => (s, t.left)
    }

  // What we're doing here is effectively a reimplementation of IO with a meaningful IvoryTower
  // and trapped exceptions. So to be good API citizens we just lift it into IO and nobody will
  // ever know (shh!).
  def iorun[A](s: State, a: Action[A]): IO[(State, Throwable \/ A)] =
    IO(run(s, a))

  ////// COMBINATORS

  // Low-level constructor. This exposes the implementation, so we hold it close.
  private def action[A](f: State => (State, Throwable \/ A)): Action[A] = 
    Suspend(Op(f(_).map(_.rightMap(Return(_)))))

  // Logical constructors
  def success[A](a: => A): Action[A] = action(s => (s, a.right))
  def fail[A](t: => Throwable): Action[A] = action(s => (s, t.left))

  // Managed resources
  def resource[R,A](acquire: Action[R])(use: R => Action[A])(dispose: R => Action[Unit]): Action[A] =
    acquire >>= { r =>
      action { s0 =>
        val (s1, e) = run(s0, use(r)) 
        run(s1, dispose(r) >> e.fold(fail(_), success(_)))
      }
    }

  // Raw combinators; these expose the state, which will have more structure in subclasses 
  // that might wish to define their own get, mod, etc. So we namespace them.
  object raw {
    def gets[T](f: State => T): Action[T] = get.map(f)
    def get: Action[State] = action(s => (s, s.right))
    def mod(f: State => State): Action[Unit] = action(s => (f(s), ().right))
    def put(s: State): Action[Unit] = mod(_ => s)
  }

}


trait ReaderWriterStateFailWorld extends FailWorld {

  // Our state is a structure with reader, writer, and state
  type R
  type W = Vector[String]
  type S

  // Thus
  case class State(r: R, w: W, s: S)

  ////// COMBINATORS

  // Reader
  def ask: Action[R] = raw.gets(_.r)

  // Writer
  def tell(w: W): Action[Unit] = raw.mod(x => x.copy(w = x.w |+| w))

  // State
  def get: Action[S] = raw.gets(_.s)
  def gets[T](f: S => T): Action[T] = get.map(f)
  def mod(f: S => S): Action[Unit] = raw.mod(x => x.copy(s = f(x.s)))
  def put(s: S): Action[Unit] = mod(_ => s)

  ////// SYNTAX

  // If our writer is a monad, we can write elements of its parameter. For some reason I can't move
  // the type constraint up here so it's on each method, sorry.
  implicit class WriterOps[A](a: Action[A]) {

    /** Log after running `a`. */
    def :++>[M[_],L](l: => L)(implicit ev: M[L] =:= W, M: Monad[M]): Action[A] =
      a.flatMap(x => tell(M.point(l)).map(_ => x))

    /** Log after running `a`, using its result. */
    def :++>>[M[_],L](f: A => L)(implicit ev: M[L] =:= W, M: Monad[M]): Action[A] =
      a.flatMap(x => tell(M.point(f(x))).map(_ => x))

    /** Log before running `a`. */
    def :<++[M[_],L](l: => L)(implicit ev: M[L] =:= W, M: Monad[M]): Action[A] =
      tell(M.point(l)).flatMap(_ => a)
  
  }

}




