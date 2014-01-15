package doobie.free
 
import scala.annotation.tailrec

import scalaz.{Free, Functor, Monad, \/, \/-, -\/}, \/._, Free._
import scalaz.std.function._
import scalaz.syntax.monad._
import scalaz.syntax.id._
import scalaz.effect.IO

import java.sql.ResultSet


// Our result type
sealed trait SqlResult[+A]
sealed trait SqlFailure extends SqlResult[Nothing]
object SqlResult {

  final case class Success[A](a: A) extends SqlResult[A]
  final case class Failure(t: Throwable) extends SqlFailure

  // This means we successfully computed an A but an invariant was broken.
  final case class Disappointment[A](a: A) extends SqlFailure

  // TODO: this is a monad (at least)
}


sealed abstract class ResultSetOpF[+A]
object ResultSetOpF {
  import resultset.ResultSetOp

  // Can't implement a transformer so we need primitives for these effects.
  final case class LiftIO[A](io: IO[A]) extends ResultSetOpF[A]
  final case class Fail[A](f: SqlFailure) extends ResultSetOpF[A]
  final case class Except[A](a: ResultSetOp[A], h: SqlFailure => ResultSetOp[A]) extends ResultSetOpF[A]

  // Constructors (in real life there are 192 of these)
  final case class Close[A](k: Unit => A) extends ResultSetOpF[A]
  final case class GetInt1[A](p0: Int, k: Int => A) extends ResultSetOpF[A]
  final case class GetString[A](p0: Int, k: java.lang.String => A) extends ResultSetOpF[A]
  final case class Next[A](k: Boolean => A) extends ResultSetOpF[A]
  final case class WasNull[A](k: Boolean => A) extends ResultSetOpF[A]

  // Functor instance
  implicit def functorResultSetOpF: Functor[ResultSetOpF] =
    new Functor[ResultSetOpF] {
      def map[A,B](fa: ResultSetOpF[A])(f: A => B): ResultSetOpF[B] =
        fa match {
        
          // MonadIO
          case LiftIO(a) => LiftIO(a map f)
          case Fail(a) => Fail(a)
          case Except(a, h) => Except(a map f, h map (_ map f))

          // Standard Constructors
          case Close(k) => Close(f compose k)
          case GetInt1(p0, k) => GetInt1(p0, f compose k)
          case GetString(p0, k) => GetString(p0, f compose k)
          case Next(k) => Next(f compose k)
          case WasNull(k) => WasNull(f compose k)

        }
    }

}

// Client module
object resultset {
  import ResultSetOpF._
  import SqlResult._

  // Free gives us a monad
  type ResultSetOp[+A] = Free[ResultSetOpF, A]
  Monad[ResultSetOp] // thus

  // Effect primitives
  def liftIO[A](a: IO[A]): ResultSetOp[A] =
    Suspend(LiftIO(a.map(Return(_))))

  def fail(f: SqlFailure): ResultSetOp[Nothing] =
    Suspend(Functor[ResultSetOpF].map(Fail(f))(Return(_))) // odd

  def except[A, B](a: ResultSetOp[A], h: SqlFailure => ResultSetOp[A]): ResultSetOp[A] =
    Suspend(Except(a.map(Return(_)), h.map(_.map(Return(_)))))

  // Derived
  def ensuring[A, B](a: ResultSetOp[A], h: ResultSetOp[B]): ResultSetOp[A] =
    except(a, f => h *> fail(f)) <* h



  // Client constructors, 1:1 with java.sql.ResultSet (192 in total, including overloading)
  def close: ResultSetOp[Unit] = Suspend(Close(Return(_)))
  def getInt(p0: Int): ResultSetOp[Int] = Suspend(GetInt1(p0, Return(_)))
  def getString(p0: Int): ResultSetOp[java.lang.String] = Suspend(GetString(p0, Return(_)))
  def next: ResultSetOp[Boolean] = Suspend(Next(Return(_)))
  def wasNull: ResultSetOp[Boolean] = Suspend(WasNull(Return(_)))

  // Our running state
  case class State(rs: ResultSet) // todo: add a log zipper and constructors for log actions

  // Each time we turn the crank we trap exceptions, both in resume (if map throws, for instance)
  // and in the implementation of each op. This allows us to preserve and return the last known
  // good state along with the failure. No tailrec with fold so we must use pattern matching here.
  @tailrec final def unsafeRunResultSet[A](a: ResultSetOp[A], s: State): (SqlResult[A], State) =
    \/.fromTryCatch(a.resume) match { 
      case \/-(-\/(op)) => 
        \/.fromTryCatch(next(s, op)) match {
          case \/-((Success(r), s0))            => unsafeRunResultSet(r, s0)
          case \/-((d @ Disappointment(_), s0)) => (d, s0)
          case \/-((f @ Failure(_), s0))        => (f, s0)
          case -\/(t)                           => (Failure(t), s)
        }
      case \/-(\/-(a)) => (Success(a), s)
      case -\/(t)      => (Failure(t), s)
    }

  // Evaluate a primitive
  def next[A](s: State, op: ResultSetOpF[A]): (SqlResult[A], State) =
    op match {

      // Effect primitives
      case LiftIO(a) => (Success(a.unsafePerformIO), s)
      case Fail(f)   => (f, s)

      // This adds a stack frame. Is there a better option here?
      case Except(a, h) => 
        unsafeRunResultSet(a, s) match { // N.B this adds a stackframe
          case (r @ Success(_), s)        => (r, s)
          case (f @ Failure(_), s)        => unsafeRunResultSet(h(f), s) // with new state
          case (d @ Disappointment(_), s) => unsafeRunResultSet(h(d), s) // with new state
          case _ => ???
        }

      // Resultset Ops
      case Close(k)         => (Success(k(s.rs.close())), s)
      case GetInt1(p0, k)   => (Success(k(s.rs.getInt(p0))), s)
      case GetString(p0, k) => (Success(k(s.rs.getString(p0))), s)
      case Next(k)          => (Success(k(s.rs.next())), s)
      case WasNull(k)       => (Success(k(s.rs.wasNull())), s)

    }


}








