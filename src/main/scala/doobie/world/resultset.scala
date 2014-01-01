package doobie
package world

import doobie.util._
import doobie._
import java.sql.{ ResultSet, SQLException }
import scalaz._
import Scalaz._
import scalaz.stream._
import scalaz.concurrent._

object resultset extends RWSFWorld with EventLogging with IndexedState {
  import rwsfops._

  protected type R = ResultSet

  sealed trait Event
  case class Error(t: Throwable) extends Event
  case class Get[A](n: Int, p: Primitive[A], a: Option[A]) extends Event
  case class WasNull(b: Boolean) extends Event
  case object Next extends Event
  case object Done extends Event
  case class Read[A:Composite](c: Composite[A], a: A) extends Event

  protected def failEvent(t: Throwable): Event =
    Error(t)

  /** Read primitive type `A` at index `n`. */
  def readN[A](n: Int)(implicit A: Primitive[A]): Action[A] =
    ask >>= { (rs: ResultSet) => 
      try {
        val a = A.get(rs)(n) // force
        unit(a) :++>> (a => Get[A](n, A, Some(a)))
      } catch {
        case sqle: SQLException => 
          tell(Vector(Get[A](n, A, None))) >> fail(sqle)
      }
    }

  /** True if the last read was a SQL NULL value. */
  def wasNull: Action[Boolean] =
    asks(_.wasNull) :++>> (WasNull(_))

  /** Advance to the next row, returning `false` if none. */
  def next: Action[Boolean] =
    asks(_.next) :++>> (a => if (a) Next else Done)

  /** Read primitive type `A` at the current index. */
  def read[A: Primitive]: Action[A] =
    get >>= (n => readN[A](n))

  /** Read composite type `A` at the current index. */
  def readC[A](implicit A : Composite[A]): Action[A] =
    A.get :++>> (a => Read(A, a))

  /** Returns a stream processor for handling results. */
  def stream[O: Composite]: Result[O,O] =
    Result.id

  implicit class ResultSetActionOps[A](a: Action[A]) {

    /** Lift this action into statement world. */
    def run: statement.Action[A] =
      statement.executeQuery(runrw(_, a))

  }

  // Result stream, with chainable tansformations and terminal folds.
  class Result[I: Composite, O](p0: Process1[I,O]) {
    import Process.{repeatEval, End}
    import Task.delay

    // TODO: straighten this out, it's kind of a mess
    protected def unsafeHead: Action[O] = ask >>= { rs =>

      var log: W = Monoid[W].zero
      def f(w: W): Unit = log = log |+| w

      def process(rs: ResultSet)(f: W => Unit): Process[Task, I] =
        repeatEval(delay { 
          runrw(rs, next) match {
            case (w, -\/(t)) => f(w); throw t
            case (w, \/-(b)) => f(w); if (b) unsafeRun(rs)(f) else throw End 
          }
        }) 

      def unsafeRun(rs: ResultSet)(f: W => Unit): I = {
        val (w, e) = runrw(rs, readC[I])
        f(w) 
        e.fold(throw _, identity)
      }

      val (w, e) = runrw(rs, asks(process(_)(f).pipe(p0)).map(_.runLast.run.get))
      val w0 = if (log.lastOption eq /** FIX THIS */ w.headOption) log else log |+| w
      gosub((w0, e), identity[W])
    }

    // the fundamental op?
    def pipe[O2](transform: Process1[O, O2]): Result[I, O2] = 
      new Result(p0 pipe transform)

    ////// Transform

    def collect[O2](f: PartialFunction[O, O2]) =
      pipe(process1.collect(f))

    def drop(n: Int): Result[I, O] =
      pipe(process1.drop(n))

    def dropWhile(f: O => Boolean): Result[I, O] =
      pipe(process1.dropWhile(f))

    def filter(f: O => Boolean): Result[I, O] =
      pipe(process1.filter(f))

    def map[O2](f: O => O2): Result[I, O2] =
      pipe(process1.lift(f))

    def scanLeft[O2](z: O2)(f: (O2,O) => O2): Result[I, O2] =
      pipe(process1.scan(z)(f))

    def take(n: Int): Result[I, O] =
      pipe(process1.take(n))

    def takeWhile(f: O => Boolean): Result[I, O] =
      pipe(process1.takeWhile(f))

    ////// Effect

    // def traverse_(f: O => IO[Unit]): Action[Unit] =


    ////// Fold

    def exists(f: O => Boolean): Action[Boolean] =
      forall(o => !f(o)).map(!_) // N.B. this is on the stream tip

    def forall(f: O => Boolean): Action[Boolean] =
      pipe(process1.forall(f)).unsafeHead

    def find(f: O => Boolean): Action[Option[O]] =
      pipe(process1.find(f)).headOption

    def foldLeft[O2](z: O2)(f: (O2,O) => O2): Action[O2] =
      pipe(process1.fold(z)(f)).unsafeHead

    def foldMap[O2](f: O => O2)(implicit M: Monoid[O2]): Action[O2] =
      pipe(process1.foldMap(f)).unsafeHead

    def headOption: Action[Option[O]] =
      pipe(process1.awaitOption).unsafeHead

    def lastOption: Action[Option[O]] =
      pipe(process1.last).headOption

    ////// CONVENIENCE FOLDS

    def toVector: Action[Vector[O]] =
      foldMap(Vector(_))

    def toList: Action[List[O]] =
      toVector.map(_.toList)

  }

  object Result extends ResultInstances {

    def id[A: Composite]: Result[A,A] =
      new Result(process1.id)

  }

  trait ResultInstances {

    implicit def functor[I]: Functor[({type λ[α] = Result[I,α]})#λ] =
      new Functor[({type λ[α] = Result[I,α]})#λ] {
        def map[A, B](fa: Result[I,A])(f: A => B): Result[I,B]  =
          fa map f
      }

  }

}

