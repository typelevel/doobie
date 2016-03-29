package doobie.hi

import doobie.enum.holdability._
import doobie.enum.transactionisolation._
import doobie.enum.fetchdirection._

import doobie.syntax.catchable.ToDoobieCatchableOps._

import doobie.free.{ connection => C }
import doobie.free.{ preparedstatement => PS }
import doobie.free.{ callablestatement => CS }
import doobie.free.{ resultset => RS }
import doobie.free.{ statement => S }
import doobie.free.{ databasemetadata => DMD }

import doobie.util.composite._
import doobie.util.invariant._

import java.net.URL
import java.util.{ Date, Calendar }
import java.sql.{ ParameterMetaData, ResultSet, ResultSetMetaData, SQLWarning, Time, Timestamp, Ref, RowId }

import scala.collection.immutable.Map
import scala.collection.JavaConverters._
import scala.collection.generic.CanBuildFrom
import scala.Predef.intArrayOps

import scalaz.{ Monad, MonadPlus, IList, NonEmptyList }
import scalaz.syntax.id._
import scalaz.syntax.monadPlus._
import scalaz.stream.Process

/**
 * Module of high-level constructors for `ResultSetIO` actions.
 * @group Modules
 */
object resultset {

  /** @group Typeclass Instances */
  implicit val CatchableResultSetIO = RS.CatchableResultSetIO

  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): ResultSetIO[A] =
    RS.delay(a)

  /** @group Cursor Control */
  def absolute(row: Int): ResultSetIO[Boolean] =
    RS.absolute(row)

  /** @group Cursor Control */
  val afterLast: ResultSetIO[Unit] =
    RS.afterLast

  /** @group Cursor Control */
  val beforeFirst: ResultSetIO[Unit] =
    RS.beforeFirst

  /** @group Updating */
  val cancelRowUpdates: ResultSetIO[Unit] =
    RS.cancelRowUpdates

  /** @group Warnings */
  val clearWarnings: ResultSetIO[Unit] =
    RS.clearWarnings

  /** @group Updating */
  val deleteRow: ResultSetIO[Unit] =
    RS.deleteRow

  /** @group Cursor Control */
  val first: ResultSetIO[Boolean] =
    RS.first

  /** 
   * Read a value of type `A` starting at column `n`.
   * @group Results 
   */
  def get[A](n: Int)(implicit A: Composite[A]): ResultSetIO[A] =
    A.get(n)

  /** 
   * Read a value of type `A` starting at column 1.
   * @group Results 
   */
  def get[A](implicit A: Composite[A]): ResultSetIO[A] =
    get(1)

  /**
   * Consumes the remainder of the resultset, reading each row as a value of type `A` and 
   * accumulating them in an `IList`.
   * @group Results 
   */
  def ilist[A](implicit A: Composite[A]): ResultSetIO[IList[A]] = 
    RS.raw { rs =>
      var as = IList.empty[A]
      while (rs.next)
        as ::= A.unsafeGet(rs, 1)
      as.reverse
    }

  /**
   * Consumes the remainder of the resultset, reading each row as a value of type `A` and 
   * accumulating them in a standard library collection via `CanBuildFrom`.
   * @group Results
   */
  def build[F[_], A](implicit C: CanBuildFrom[Nothing, A, F[A]], A: Composite[A]): ResultSetIO[F[A]] =
    RS.raw { rs =>
      val b = C()
      while (rs.next)
        b += A.unsafeGet(rs, 1)
      b.result()
    }

  /**
   * Consumes the remainder of the resultset, reading each row as a value of type `A`, mapping to
   * `B`, and accumulating them in a standard library collection via `CanBuildFrom`. This unusual
   * constructor is a workaround for the CanBuildFrom not having a sensible contravariant functor
   * instance.
   * @group Results
   */
  def buildMap[F[_], A, B](f: A => B)(implicit C: CanBuildFrom[Nothing, B, F[B]], A: Composite[A]): ResultSetIO[F[B]] =
    RS.raw { rs =>
      val b = C()
      while (rs.next)
        b += f(A.unsafeGet(rs, 1))
      b.result()
    }

  /**
   * Consumes the remainder of the resultset, reading each row as a value of type `A` and 
   * accumulating them in a `Vector`.
   * @group Results 
   */
  def vector[A: Composite]: ResultSetIO[Vector[A]] = 
    build[Vector, A]

  /**
   * Consumes the remainder of the resultset, reading each row as a value of type `A` and 
   * accumulating them in a `List`.
   * @group Results 
   */
  def list[A: Composite]: ResultSetIO[List[A]] = 
    build[List, A]

  /**
   * Like `getNext` but loops until the end of the resultset, gathering results in a `MonadPlus`.
   * @group Results
   */
  def accumulate[G[_]: MonadPlus, A: Composite]: ResultSetIO[G[A]] =
    get[A].whileM(next)

  /** 
   * Updates a value of type `A` starting at column `n`.
   * @group Updating 
   */
  def update[A](n: Int, a:A)(implicit A: Composite[A]): ResultSetIO[Unit] =
    A.update(n, a)

  /** 
   * Updates a value of type `A` starting at column 1.
   * @group Updating 
   */
  def update[A](a: A)(implicit A: Composite[A]): ResultSetIO[Unit] =
    A.update(1, a)


  /** 
   * Similar to `next >> get` but lifted into `Option`; returns `None` when no more rows are
   * available.
   * @group Results 
   */
  def getNext[A: Composite]: ResultSetIO[Option[A]] =
    next >>= {
      case true  => get[A].map(Some(_))
      case false => Monad[ResultSetIO].point(None)
    }
    
  /** 
   * Equivalent to `getNext`, but verifies that there is exactly one row remaining.
   * @throws `UnexpectedCursorPosition` if there is not exactly one row remaining
   * @group Results 
   */
  def getUnique[A: Composite]: ResultSetIO[A] =
    (getNext[A] |@| next) {
      case (Some(a), false) => a
      case (Some(a), true)  => throw UnexpectedContinuation
      case (None, _)        => throw UnexpectedEnd
    }

  /**
   * Equivalent to `getNext`, but verifies that there is at most one row remaining.
   * @throws `UnexpectedContinuation` if there is more than one row remaining
   * @group Results
   */
  def getOption[A: Composite]: ResultSetIO[Option[A]] =
    (getNext[A] |@| next) {
      case (a @ Some(_), false) => a
      case (Some(a), true)      => throw UnexpectedContinuation
      case (None, _)            => None
    }

  /**
    * Consumes the remainder of the resultset, but verifies that there is at least one row remaining.
    * @throws `UnexpectedEnd` if there is not at least one row remaining
    * @group Results
    */
  def nel[A: Composite]: ResultSetIO[NonEmptyList[A]] =
    (getNext[A] |@| ilist) {
      case (Some(a), as) => NonEmptyList.nel(a, as)
      case (None, _)     => throw UnexpectedEnd
    }

  /** 
   * Process that reads from the `ResultSet` and returns a stream of `A`s. This is the preferred
   * mechanism for dealing with query results.
   * @group Results 
   */
  def process[A: Composite]: Process[ResultSetIO, A] = 
    Process.repeatEval(getNext[A]).takeWhile(_.isDefined).map(_.get)

  /** @group Properties */
  val getFetchDirection: ResultSetIO[FetchDirection] =
    RS.getFetchDirection.map(FetchDirection.unsafeFromInt)

  /** @group Properties */
  val getFetchSize: ResultSetIO[Int] =
    RS.getFetchSize

  /** @group Properties */
  val getHoldability: ResultSetIO[Holdability] =
    RS.getHoldability.map(Holdability.unsafeFromInt)

  /** @group Properties */
  val getMetaData: ResultSetIO[ResultSetMetaData] =
    RS.getMetaData

  /** @group Cursor Control */
  val getRow: ResultSetIO[Int] =
    RS.getRow

  /** @group Warnings */
  val getWarnings: ResultSetIO[Option[SQLWarning]] =
    RS.getWarnings.map(Option(_))

  /** @group Updating */
  val insertRow: ResultSetIO[Unit] =
    RS.insertRow

  /** @group Cursor Control */
  val isAfterLast: ResultSetIO[Boolean] =
    RS.isAfterLast

  /** @group Cursor Control */
  val isBeforeFirst: ResultSetIO[Boolean] =
    RS.isBeforeFirst

  /** @group Cursor Control */
  val isFirst: ResultSetIO[Boolean] =
    RS.isFirst

  /** @group Cursor Control */
  val isLast: ResultSetIO[Boolean] =
    RS.isLast

  /** @group Cursor Control */
  val last: ResultSetIO[Boolean] =
    RS.last

  /** @group Cursor Control */
  val moveToCurrentRow: ResultSetIO[Unit] =
    RS.moveToCurrentRow

  /** @group Cursor Control */
  val moveToInsertRow: ResultSetIO[Unit] =
    RS.moveToInsertRow

  /** @group Cursor Control */
  val next: ResultSetIO[Boolean] =
    RS.next

  /** @group Cursor Control */
  val previous: ResultSetIO[Boolean] =
    RS.previous

  /** @group Cursor Control */
  val refreshRow: ResultSetIO[Unit] =
    RS.refreshRow

  /** @group Cursor Control */
  def relative(n: Int): ResultSetIO[Boolean] =
    RS.relative(n)

  /** @group Cursor Control */
  val rowDeleted: ResultSetIO[Boolean] =
    RS.rowDeleted

  /** @group Cursor Control */
  val rowInserted: ResultSetIO[Boolean] =
    RS.rowInserted

  /** @group Cursor Control */
  val rowUpdated: ResultSetIO[Boolean] =
    RS.rowUpdated

  /** @group Properties */
  def setFetchDirection(fd: FetchDirection): ResultSetIO[Unit] =
    RS.setFetchDirection(fd.toInt)

  /** @group Properties */
  def setFetchSize(n: Int): ResultSetIO[Unit] =
    RS.setFetchSize(n)

}
