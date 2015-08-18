package doobie.util

import doobie.util.capture.Capture
import doobie.free.connection.ConnectionIO
import doobie.util.liftxa.LiftXA
import doobie.util.connector.Connector
import doobie.util.yolo.Yolo
import doobie.hi.connection.ProcessConnectionIOOps

import scalaz.{ Monad, Catchable, @>, ~>, Lens }
import scalaz.syntax.monad._
import scalaz.stream.Process
import scalaz.stream.Process.eval

import java.sql.Connection

object transactor {

  /** Specialization of `Connection` for data types that also carry a replaceable `LiftXA`. */
  trait Transactor[M[_], A] extends Connector[M, A] {
    def liftXA: A @> LiftXA
  }

  object Transactor {

    /** A default instance, given constraints on M. */
    def instance[M[_]: Monad: Capture: Catchable, A](lens: A @> LiftXA, f: A => M[Connection]): Transactor[M, A] =
      new Transactor[M, A] {
        def liftXA = lens
        def connect(a: A): M[Connection] = f(a)
        def trans(a: A): ConnectionIO ~> M =
          new (ConnectionIO ~> M) {        
            def apply[B](ma: ConnectionIO[B]) = connect(a) >>= ma.transK[M]      
          }
        def transP(a: A): Process[ConnectionIO, ?] ~> Process[M, ?] =
          new (Process[ConnectionIO, ?] ~> Process[M, ?]) {
            def apply[B](pa: Process[ConnectionIO, B]) = eval(connect(a)) >>= pa.trans[M]    
          }
      }

    /** Retrieve the `liftXA` lens. */
    def liftXA[M[_], T](implicit ev: Transactor[M, T]): T @> LiftXA = 
      ev.liftXA

    /** Wrap a `ConnectionIO` in before/after/oops/always logic. */
    def safe[M[_], T, A](t: T)(ma: ConnectionIO[A])(implicit ev: Transactor[M, T]): ConnectionIO[A] =
      liftXA.get(t).safe(ma)

    /** Wrap a `Process[ConnectionIO, ?]` in before/after/oops/always logic. */
    def safeP[M[_], T, A](t: T)(pa: Process[ConnectionIO, A])(implicit ev: Transactor[M, T]): Process[ConnectionIO, A] =
      liftXA.get(t).safeP(pa)

  }
  
}
