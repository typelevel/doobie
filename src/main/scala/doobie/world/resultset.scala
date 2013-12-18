package doobie
package world

import doobie.util._
import doobie._
import java.sql.ResultSet
import scalaz._
import Scalaz._
import scalaz.stream._
import scalaz.concurrent._

object resultset extends RWSFWorld with EventLogging with IndexedState {
  import rwsfops._

  protected type R = ResultSet

  /** Read primitive type `A` at index `n`. */
  def readN[A](n: Int)(implicit A: Primitive[A]): Action[A] =
    asks(A.get(_)(n)) :++>> (a => s"GET $n ${A.jdbcType.name} => $a")

  /** True if the last read was a SQL NULL value. */
  def wasNull: Action[Boolean] =
    asks(_.wasNull) :++>> (a => s"WAS NULL => $a")

  /** Advance to the next row, returning `false` if none. */
  def next: Action[Boolean] =
    asks(_.next) :++>> (a => s"NEXT => $a")

  /** Read primitive type `A` at the current index. */
  def read[A: Primitive]: Action[A] =
    get >>= (n => readN[A](n))

  /** Read composite type `A` at the current index. */
  def readC[A](implicit A : Composite[A]): Action[A] =
    A.get

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

    ////// IMPLEMENTATION DETAILS

    // TODO: we're discarding the log here. figure out how to preserve
    private def process(rs: ResultSet): Process[Task, I] =
      repeatEval(delay(if (rs.next) unsafeRun(rs) else throw End)) 

    // Read a row. This isn't quite what we want. See above.
    private def unsafeRun(rs: ResultSet): I =
      runrw(rs, readC[I])._2.fold(throw _, identity)


    // Ultimately the stream is always folded up into a single value, so here's how
    // we get it out. Callers must ensure the stream isn't empty.
    protected def unsafeHead: Action[O] = 
      asks(process(_).pipe(p0)).map(_.runLast.run.get)

    // the fundamental op?
    def pipe[O2](transform: Process1[O, O2]): Result[I, O2] = 
      new Result(p0 pipe transform)


    ////// SECOND ATTEMPT

    // Perhaps change this to (W, Option[Throwable]) \/ I and stop at the first 
    // writer emit that's a throwable. This would allow us to translate back 
    // to (W, Throwable \/ A) and would permit pipeO 

    // ... but we really don't want to accumulate per-row log information; we want a rollup with
    // timing and counts/stats per row, and the full writer state for any failed row. So within
    // this stuff we probably want a different state strategy.

    private def process2(rs: ResultSet): Process[Task, (W, Throwable \/ I)] = {
      var w = Monoid[W].zero // State accumulation is a cheat
      repeatEval(delay {
        if (rs.next) runrw(rs, readC[I]).leftMap { w0 => w = w |+| w0; w }
        else throw End
      }) |> process1.takeThrough(_._2.isRight) // Halt if we encounter a failure
    }


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

