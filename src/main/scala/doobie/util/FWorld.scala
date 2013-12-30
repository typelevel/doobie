package doobie
package util

import scala.annotation.tailrec
import scalaz._
import scalaz.Scalaz._
import scalaz.Free._
import scalaz.effect._
import language.higherKinds

/** 
 * Fail world; this is `State` implemented in `Free` with state-preserving exception handling. The 
 * state itself is an existential that can remain private to the implementation. This abstracts the
 * common pattern in `Free` where we want to encapsulate mutable state.
 */
trait FWorld {

  // Our state is not necessarily public
  protected type State

  // Our algebra is trivial; all operations are state transitions.
  private case class Op[+A](f: State => (State, Throwable \/ A))

  // A functor here gives us a Free monad.
  private implicit val OpFunctor: Functor[Op] =  
    new Functor[Op] {
      def map[A, B](op: Op[A])(g: A => B) = 
        Op(op.f(_).map(_.rightMap(g)))
    }

  // Thus. 
  private type RawAction[+A] = Free[Op, A]
  Monad[RawAction] // proof

  // We hide the Free implementation. Is this problematic? I don't think so
  final class Action[+A](protected[FWorld] val a: RawAction[A]) 

  object Action {

    implicit val monad: Monad[Action] =
      new Monad[Action] {
        
        def point[A](a: => A): Action[A] =
          new Action(a.point[RawAction])
      
        def bind[A, B](fa: Action[A])(f: A => Action[B]): Action[B] =
          new Action(fa.a.flatMap(f(_).a))

        override def map[A, B](fa: Action[A])(f: A => B): Action[B] =
          new Action(fa.a map f)

      }

  }



  ////// INTERPRETER

  // Each time we turn the crank we trap exceptions, both in resume (if map throws, for instance)
  // and in the implementation of each Op. This allows us to preserve and return the last known
  // good state along with the failure. No tailrec with fold so we must use pattern matching here.
  @tailrec protected final def runll[A](s: State, a: RawAction[A]): (State, Throwable \/ A) = 
    \/.fromTryCatch(a.resume) match {
      case \/-(-\/(Op(f))) =>
        \/.fromTryCatch(f(s)) match {
          case \/-((s, \/-(a))) => runll(s, a)
          case \/-((s, -\/(t))) => (s, t.left)
          case -\/(t) => (failState(s, t), t.left)
        }
      case \/-(\/-(a)) => (s, a.right)
      case -\/(t)      => (failState(s, t), t.left)
    }

  protected def runf[A](s: State, a: Action[A]): (State, Throwable \/ A) = 
    runll(s, a.a)

  protected def failState(s:State, t: Throwable): State

  ////// ACTIONS

  // Low-level constructor. This exposes the implementation, so we hold it close.
  private def action[A](f: State => (State, Throwable \/ A)): Action[A] = 
    new Action(Suspend(Op(f(_).map(_.rightMap(Return(_))))))

  // Unit operations are public
  def unit[A](a: => A): Action[A] = action(s => (s, a.right))
  def fail(t: => Throwable): Action[Nothing] = action(s => (s, t.left))

  // Low-level primitives; these expose the state, which will have more structure in subclasses 
  // that might wish to define their own get, mod, etc. So we just namespace them.
  protected object fops {

    // State operations
    def gets[T](f: State => T): Action[T] = get.map(f)
    def get: Action[State] = action(s => (s, s.right))
    def mod(f: State => State): Action[Unit] = action(s => (f(s), ().right))
    def put(s: State): Action[Unit] = mod(_ => s)

    // Managed resource; `dispose` will run whether or not `use` completes successfully.
    def resource[R,A](acquire: Action[R], use: R => Action[A], dispose: R => Action[Unit]): Action[A] =
      acquire >>= { r =>
        action { s0 =>
          val (s1, e) = runf(s0, use(r)) 
          runf(s1, dispose(r) >> e.fold(fail(_), unit(_)))
        }
      }

  }

}




