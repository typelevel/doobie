package doobie.free
 
import scala.annotation.tailrec

import scalaz.{ Free, Functor, Monad, \/, \/-, -\/, Tree }, \/._, Free._
import scalaz.std.function._
import scalaz.std.tuple._
import scalaz.syntax.monad._
import scalaz.syntax.bifunctor._
import scalaz.syntax.id._
import scalaz.effect.IO

import shapeless._

import java.sql.ResultSet

sealed abstract class ResultSetOpF[+A]
object ResultSetOpF {

  // Can't implement a transformer so we need primitives for these effects.
  final case class LiftIO[A, B](io: IO[A], k: A => B) extends ResultSetOpF[B]
  final case class Fail[A](f: SqlFailure) extends ResultSetOpF[A]
  final case class Except[A](a: ResultSetOp[A], handler: SqlFailure => ResultSetOp[A]) extends ResultSetOpF[A]

  final case class Log[A](label: String, a: ResultSetOp[A]) extends ResultSetOpF[A]

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
          case LiftIO(a, k) => LiftIO(a, f compose k)
          case Fail(a) => Fail(a)
          case Except(a, h) => Except(a map f, h map (_ map f))
          case Log(s, a) => Log(s, a map f)

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

  import ResultSetOpF._, SqlResult._

  // Our running state
  case class State(zip: LogZipper, rs: ResultSet) // todo: add a log zipper and constructors for log actions
  object State {
    val zip = Lens[State] >> 0
  }

  // In order to maintain tailrec later on we do our work outside IO and unsafe any liftIOs we find. 
  // We then wrap the invocation back in IO so the outer run is pure.
  def run[A](a: ResultSetOp[A], s: State): IO[(SqlResult[A], State)] = 
    IO(unsafeRun(a, s))

  // Each time we turn the crank we trap exceptions, both in resume (if map throws, for instance)
  // and in the implementation of each op. This allows us to preserve and return the last known
  // good state along with the failure. No tailrec with fold so we must use pattern matching here.
  // *** Side-effecting; calls should be thunked in IO.
  @tailrec private def unsafeRun[A](a: ResultSetOp[A], s: State): (SqlResult[A], State) = {
    \/.fromTryCatch(a.resume) match { 
      case \/-(-\/(op)) => 

        val entry = LogEntry.Pending(Coproduct[GenOp](op : ResultSetOpF[Any]), System.nanoTime)
        val s0 = State.zip.modify(s)(_.insertDownLast(Tree(entry)))
        val (r, s1) = \/.fromTryCatch(unsafeNext(op, s0)).fold(t => (Failure(t), s0), identity)
        val label = (r, s1.zip.getLabel) match {
          case (Success(_), LogEntry.Computed(op, a, ns)) => LogEntry.Complete(op, a, System.nanoTime - ns)
          case (f, LogEntry.Pending(op, ns)) => LogEntry.Complete(op, f, System.nanoTime - ns)
          case (x, l) => sys.error(s"illegal log state: (result, entry) = ($x, $l")
        }
        val s2 = State.zip.modify(s1)(_.setLabel(label).parent.get)

        r match {
          case Success(r) => unsafeRun(r, s2) // ** Unsafe
          case r : Failure => (r, s2)
          case r : Disappointment => (r, s2)
        }

      case \/-(\/-(a)) => 
        println("success: " + a)
        (Success(a), s)

      case -\/(t)      => 
        println("throwable: " + t)
        (Failure(t), s)

    }
  }

  // Evaluate a primitive. 
  // *** Side-effecting; calls should be thunked in IO.
  private def unsafeNext[A](op: ResultSetOpF[A], s: State): (SqlResult[A], State) = {

    def done[A, B](a: A, k: A => B, s0: State): (SqlResult[B], State) = {
      val l0 = s0.zip.getLabel
      val l1 = l0 match {
        case LogEntry.Pending(op, ns) => LogEntry.Computed(op, Success(a), ns)
        case n => sys.error("implementation bug; incorrect log node: " + n)
      }
      val s1 = State.zip.modify(s0)(_.setLabel(l1))
      (Success(k(a)), s1)
    }



    op match {

      // Effect primitives
      case LiftIO(a, k) => done(a.unsafePerformIO, k, s) //** Unsafe
      case Fail(f)   => (f, s)

      // This adds a stack frame. Is there a better option here?
      case Except(a, h) =>
        unsafeRun(a, s) match {
          case (r : SqlFailure, s0) => unsafeRun(h(r), s0) // ** Unsafe
          case ok => ok
        }

      case Log(label, a) => unsafeRun(a, s)


      // Resultset Ops ** All Unsafe
      case Close(k)         => (Success(k(s.rs.close())), s)
      case GetInt1(p0, k)   => (Success(k(s.rs.getInt(p0))), s)
      case GetString(p0, k) => (Success(k(s.rs.getString(p0))), s)
      case Next(k)          => (Success(k(s.rs.next())), s)
      case WasNull(k)       => (Success(k(s.rs.wasNull())), s)

    }
  }

  ////// SUSPENSION CONSTRUCTORS 

  // Effect primitives
  def liftIO[A](a: IO[A]): ResultSetOp[A] =
    Suspend(LiftIO(a, Return(_: A)))

  def fail(f: SqlFailure): ResultSetOp[Nothing] =
    Suspend(Functor[ResultSetOpF].map(Fail(f))(Return(_))) // odd

  def except[A](a: ResultSetOp[A], handler: SqlFailure => ResultSetOp[A]): ResultSetOp[A] =
    Suspend(Functor[ResultSetOpF].map(Except(a, handler))(Return(_)))

  def log[A](label: String, a: ResultSetOp[A]): ResultSetOp[A] =
    Suspend(Functor[ResultSetOpF].map(Log(label, a))(Return(_)))

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

}





