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
  protected def runrws[A](r: R, s:S, a: Action[A]): (W, S, Throwable \/ A) = {
    val (s0, e) = runf(State(r, W.zero, s), a)
    (s0.w, s0.s, e)
  }

  ////// ACTIONS (namespaced and protected; implementors may not want to expose any of them)

  protected object rwsfops {

    // Reader
    def ask: Action[R] = fops.gets(_.r)
    def asks[A](f: R => A): Action[A] = ask.map(f)

    // Writer
    def tell(w: W): Action[Unit] = fops.mod(x => x.copy(w = x.w |+| w))

    // State
    def get: Action[S] = fops.gets(_.s)
    def gets[A](f: S => A): Action[A] = get.map(f)
    def mod(f: S => S): Action[Unit] = fops.mod(x => x.copy(s = f(x.s)))
    def put(s: S): Action[Unit] = mod(_ => s)

    // Lift an arbitrary computation of the same shape and Writer type as our computations. This is
    // designed to allow lifting from other worlds.
    def gosub[W0, A](run: => (W0, Throwable \/ A), log: W0 => W): Action[A] =
      unit(run) >>= { case (w, e) => tell(log(w)) >> e.fold(fail(_), unit(_)) }

  }

}


