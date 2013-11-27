package doobie
package util

import scala.annotation.tailrec
import scalaz._
import scalaz.Scalaz._
import scalaz.Free._
import scalaz.effect._
import language.higherKinds

/** 
 * A fail world encoding a reader `R` for configuration/context, a monoidal writer `W` for 
 * accumulating a result to the side ("logging'), and arbitrary state `S`. All are existential and
 * private to the implementation by default.
 */
abstract class RWSFWorld extends FWorld { 

  protected type R // Reader, arbitrary fixed input
  protected type W // Writer, accumulated output (monoid, typically w/outer monad; ex: List[Foo]) 
  protected type S // State, arbitrary updatable input

  // Writer requires that we can combine written elements, and requires a zero to start
  protected implicit def W: Monoid[W]

  // Our state simply packs R, W, and S
  protected case class State(r: R, w: W, s: S)

  // Execution consumes the reader and produces the writer
  def runrws[A](r: R, s:S, a: Action[A]): (W, S, Throwable \/ A) = {
    val (s0, e) = runf(State(r, W.zero, s), a)
    (s0.w, s0.s, e)
  }

  // Abbreviated execution for unit state :-\
  def runrw[A](r: R, a: Action[A])(implicit ev: S =:= Unit): (W, Throwable \/ A) = {
    val (w, s, e) = runrws(r, ().asInstanceOf[S] /* careful */, a)
    (w, e)
  }

  ////// COMBINATORS (all protected; implementors may not want to expose any of them)

  // Reader
  protected def ask: Action[R] = fops.gets(_.r)
  protected def asks[A](f: R => A): Action[A] = ask.map(f)

  // Writer
  protected def tell(w: W): Action[Unit] = fops.mod(x => x.copy(w = x.w |+| w))

  // State
  protected def get: Action[S] = fops.gets(_.s)
  protected def gets[A](f: S => A): Action[A] = get.map(f)
  protected def mod(f: S => S): Action[Unit] = fops.mod(x => x.copy(s = f(x.s)))
  protected def put(s: S): Action[Unit] = mod(_ => s)

  // Lift the result from another world that shares the same type of writer, discarding the final
  // state. This is designed to be used with nested calls to `runrws`.
  protected def gosub[A](run: => (W, Throwable \/ A)): Action[A] =
    success(run) >>= { case (w, e) => tell(w) >> e.fold(fail(_), success(_)) }

  ////// SYNTAX

  // If our writer has an outer monad, we can `tell` values of its type argument. I can't figure out 
  // how to pull the constraint up here without breaking the implicit search, so the type constraint
  // appears on each operation. This just barely works. Suggestions welcome.
  protected implicit class WriterOps[A](a: Action[A]) {

    /** Log after running `a`, using its result. */
    def :++>>[M[_],L](f: A => L)(implicit ev: M[L] =:= W, M: Monad[M]): Action[A] =
      a.flatMap(x => tell(M.point(f(x))).map(_ => x))

    /** Log after running `a`. */
    def :++>[M[_],L](l: => L)(implicit ev: M[L] =:= W, M: Monad[M]): Action[A] =
      :++>>(_ => l)

    /** Log before running `a`. */
    def :<++[M[_],L](l: => L)(implicit ev: M[L] =:= W, M: Monad[M]): Action[A] =
      tell(M.point(l)).flatMap(_ => a)
  
  }

}


