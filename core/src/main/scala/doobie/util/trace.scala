package doobie.util

import doobie.free.kleislitrans._
import doobie.hi._
import doobie.util.capture.Capture
import doobie.syntax.catchable._

import scalaz.{ :+: => _, _ }, Scalaz._

import shapeless._

object trace {

  // a supertype of all the TraceOp types, parameterized on the return type
  trait TraceOp[J,A]

  // An effectful logger
  trait Trace[M[_]] {

    // log the initiation of the given operation, returning a continuation to be called
    // on completion. this may not be immediate; unless TraceOp is a primitive there will likely be 
    // intervening operations. however the contract guarantees stacklike behavior; continuations
    // will never be invoked out-of-order.
    def log[J,A](j: J, op: TraceOp[J,A]): M[Throwable \/ A => M[Unit]]

  }

  object Trace {

    /** 
     * Construct a no-op trace for `F`. This performs no allocation and should not introduce any
     * runtime overhead, heh-heh.
     */
    def empty[F[_]: Applicative]: Trace[F] =
      new Trace[F] {        
        val k    = {
          val unit = ().point[F]
          ((_ : Any) => unit).point[F]
        }
        def log[J,A](j: J, op: TraceOp[J,A]): F[Throwable \/ A => F[Unit]] =
          k.asInstanceOf[F[Throwable \/ A => F[Unit]]]
      }

    /** Construct a simple trace that logs to the console. */
    def consoleFirehose[M[_]](implicit delay: Capture[M]): Trace[M] =
      new Trace[M] {
        import Predef.augmentString
        var indent = 0
        def log[J,A](j: J, op: TraceOp[J,A]): M[Throwable \/ A => M[Unit]] =
          delay {
            Console.println(" " * indent + j.getClass.getSimpleName + " " + op)
            indent += 2
            (_ => delay(indent -= 2))
          }
      }

  }



  // typeclass-based version

  // An effectful logger
  trait Trace2[M[_]] {

    // log the initiation of the given operation, returning a continuation to be called
    // on completion. this may not be immediate; unless TraceOp is a primitive there will likely be 
    // intervening operations. however the contract guarantees stacklike behavior; continuations
    // will never be invoked out-of-order.
    def log[F[_],J,A](j: J, op: F[A])(implicit ev: KleisliTrans.Aux[F,J]): M[Throwable \/ A => M[Unit]]

  }

}
