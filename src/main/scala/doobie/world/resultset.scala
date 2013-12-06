package doobie
package world

import doobie.util._
import doobie._
import java.sql.ResultSet
import scalaz._
import Scalaz._
import scalaz.stream._
import scalaz.concurrent._

object resultset extends DWorld.Indexed {
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

  /** Returns a stream processor for handling results. */
  def stream[O: Composite]: Result[O,O] =
    new Result(process1.id)

  implicit class ResultSetActionOps[A](a: Action[A]) {

    /** Lift this action into statement world. */
    def lift: statement.Action[A] =
      statement.executeQuery(runi(_, a))

  }

  // Result stream, with chainable tansformations and terminal folds.
  class Result[I, O](p0: Process1[I,O])(implicit I : Composite[I]) {
    import Process.{repeatEval, End}
    import Task.delay

    ////// IMPLEMENTATION DETAILS

    // TODO: we're discarding the log here. figure out how to preserve
    private def process(rs: ResultSet): Process[Task, I] =
      repeatEval(delay(if (rs.next) unsafeRun(rs) else throw End)) 

    // Read a row. This isn't quite what we want. See above.
    private def unsafeRun(rs: ResultSet): I =
      runi(rs, I.get)._2.fold(throw _, identity)

    // Ultimately the stream is always folded up into a single value, so here's how
    // we get it out. Callers must ensure the stream isn't empty.
    protected def unsafeHead: Action[O] = 
      asks(process(_).pipe(p0)).map(_.runLast.run.get)

    // Let's keep this protected for now so we don't leak implementation details.
    // Might want to revisit?
    protected def pipe[O2](transform: Process1[O, O2]): Result[I, O2] = 
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

  object Result extends ResultInstances 

  trait ResultInstances {

    implicit def functor[I]: Functor[({type λ[α] = Result[I,α]})#λ] =
      new Functor[({type λ[α] = Result[I,α]})#λ] {
        def map[A, B](fa: Result[I,A])(f: A => B): Result[I,B]  =
          fa map f
      }

  }

}

