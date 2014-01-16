package doobie.free
 
import scala.annotation.tailrec

import scalaz.{Free, Functor, Monad, \/, \/-, -\/}, \/._, Free._
import scalaz.std.function._
import scalaz.std.tuple._
import scalaz.syntax.monad._
import scalaz.syntax.bifunctor._
import scalaz.syntax.id._
import scalaz.effect.IO

import java.sql.ResultSet

sealed abstract class ResultSetOpF[+A]
object ResultSetOpF {
  import resultset.ResultSetOp

  // Can't implement a transformer so we need primitives for these effects.
  final case class LiftIO[A](io: IO[A]) extends ResultSetOpF[A]
  final case class Fail[A](f: SqlFailure) extends ResultSetOpF[A]
  final case class Except[A](a: ResultSetOp[A], handler: SqlFailure => ResultSetOp[A]) extends ResultSetOpF[A]

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

  def except[A](a: ResultSetOp[A], handler: SqlFailure => ResultSetOp[A]): ResultSetOp[A] =
    Suspend(Functor[ResultSetOpF].map(Except(a, handler))(Return(_)))

  // Derived, useful
  def ensuring[A, B](a: ResultSetOp[A], h: ResultSetOp[B]): ResultSetOp[A] =
    except(a <* h, f => h *> fail(f))

  def success[A](a: A): ResultSetOp[A] =
    a.point[ResultSetOp]

  // Client constructors, 1:1 with java.sql.ResultSet (192 in total, including overloading)
  def close: ResultSetOp[Unit] = Suspend(Close(Return(_)))
  def getInt(p0: Int): ResultSetOp[Int] = Suspend(GetInt1(p0, Return(_)))
  def getString(p0: Int): ResultSetOp[java.lang.String] = Suspend(GetString(p0, Return(_)))
  def next: ResultSetOp[Boolean] = Suspend(Next(Return(_)))
  def wasNull: ResultSetOp[Boolean] = Suspend(WasNull(Return(_)))

  // Our running state
  case class State(rs: ResultSet) // todo: add a log zipper and constructors for log actions


  def run[A](a: ResultSetOp[A], s: State): IO[(SqlResult[A], State)] = {

    // In order to maintain tailrec here we do our work outside IO and unsafe any liftIOs we find. 
    // We then wrap the invocation back in IO so the outer run is pure.

    // Each time we turn the crank we trap exceptions, both in resume (if map throws, for instance)
    // and in the implementation of each op. This allows us to preserve and return the last known
    // good state along with the failure. No tailrec with fold so we must use pattern matching here.
    @tailrec def run0[A](a: ResultSetOp[A], s: State): (SqlResult[A], State) =
      \/.fromTryCatch(a.resume) match { 
        case \/-(-\/(op)) => 
          \/.fromTryCatch(next(op, s)) match {
            case \/-((Success(r), s0))         => run0(r, s0)
            case \/-((f @ Failure(_), s0))     => (f, s0)
            case \/-((d : Disappointment, s0)) => (d, s0) 
            case -\/(t)                        => (Failure(t), s)
          }
        case \/-(\/-(a)) => (Success(a), s)
        case -\/(t)      => (Failure(t), s)
      }

    // Evaluate a primitive
    def next[A](op: ResultSetOpF[A], s: State): (SqlResult[A], State) =
      op match {

        // Effect primitives
        case LiftIO(a) => (Success(a.unsafePerformIO), s)
        case Fail(f)   => (f, s)

        // This adds a stack frame. Is there a better option here?
        case Except(a, h) =>
          run0(a, s) match {
            case (r : SqlFailure, s0) => run0(h(r), s0)
            case ok => ok
          }

        // Resultset Ops
        case Close(k)         => (Success(k(s.rs.close())), s)
        case GetInt1(p0, k)   => (Success(k(s.rs.getInt(p0))), s)
        case GetString(p0, k) => (Success(k(s.rs.getString(p0))), s)
        case Next(k)          => (Success(k(s.rs.next())), s)
        case WasNull(k)       => (Success(k(s.rs.wasNull())), s)

      }

    IO(run0(a, s))

  }

}








