package doobie
package util

import scala.annotation.tailrec
import scalaz._
import scalaz.Scalaz._
import scalaz.Free._
import scalaz.effect._
import language.higherKinds

abstract class RWSFWorld[R0, W0: Monoid, S0] extends FWorld {

  // Our state is a structure with reader, writer, and state
  type R = R0
  type W = W0 
  type S = S0

  // Thus
  case class State(r: R, w: W, s: S)

  ////// COMBINATORS

  // Reader
  def ask: Action[R] = raw.gets(_.r)
  def asks[A](f: R => A): Action[A] = ask.map(f)

  // Writer
  def tell(w: W): Action[Unit] = raw.mod(x => x.copy(w = x.w |+| w))

  // State
  def get: Action[S] = raw.gets(_.s)
  def gets[A](f: S => A): Action[A] = get.map(f)
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


