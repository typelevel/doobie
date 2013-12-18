package doobie
package util

import scala.annotation.tailrec
import scalaz._
import scalaz.Scalaz._
import scalaz.Free._
import scalaz.effect._
import language.higherKinds

/** 
 * A fail world encoding a reader `R` for configuration/context and a monoidal writer `W` for 
 * accumulating a result to the side ("logging'). All are existential and private to the 
 * implementation by default.
 */
abstract class RWFWorld extends FWorld { 

  protected type R // Reader, arbitrary fixed input
  protected type W // Writer, accumulated output (monoid, typically w/outer monad; ex: List[Foo]) 

  // Writer requires that we can combine written elements, and requires a zero to start
  protected implicit def W: Monoid[W]

  // Our state simply packs R and W
  protected case class State(r: R, w: W)

  // Execution consumes the reader and produces the writer
  protected def runrw[A](r: R, a: Action[A]): (W, Throwable \/ A) = {
    val (s0, e) = runf(State(r, W.zero), a)
    (s0.w, e)
  }

  ////// ACTIONS (namespaced and protected; implementors may not want to expose any of them)

  protected object rwfops {

    // Reader
    def ask: Action[R] = fops.gets(_.r)
    def asks[A](f: R => A): Action[A] = ask.map(f)

    // Writer
    def tell(w: W): Action[Unit] = fops.mod(x => x.copy(w = x.w |+| w))

    // Lift an arbitrary computation of the same shape and Writer type as our computations. This is
    // designed to allow lifting from other worlds.
    def gosub[A](run: => (W, Throwable \/ A)): Action[A] =
      unit(run) >>= { case (w, e) => tell(w) >> e.fold(fail(_), unit(_)) }

  }

}


