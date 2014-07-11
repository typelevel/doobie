package doobie.hi

import doobie.enum.holdability._
import doobie.enum.transactionisolation._

import doobie.syntax.catchable._

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
import java.sql.{ ParameterMetaData, ResultSetMetaData, SQLWarning, Time, Timestamp, Ref, RowId }

import scala.collection.immutable.Map
import scala.collection.JavaConverters._
import scala.Predef.intArrayOps

import scalaz.Monad
import scalaz.syntax.id._
import scalaz.syntax.applicative._
import scalaz.stream.Process

/**
 * Module of high-level constructors for `ResultSetIO` actions.
 * @group Modules
 */
object resultset {

  /** @group Typeclass Instances */
  implicit val MonadResultSetIO = RS.MonadResultSetIO

  /** @group Typeclass Instances */
  implicit val CatchableResultSetIO = RS.CatchableResultSetIO

  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): ResultSetIO[A] =
    RS.delay(a)

  // /** @group Cursor Control */
  // def absolute(a: Int): ResultSetIO[Boolean] =
  //   Predef.???

  // /** @group Cursor Control */
  // val afterLast: ResultSetIO[Unit] =
  //   Predef.???

  // /** @group Cursor Control */
  // val beforeFirst: ResultSetIO[Unit] =
  //   Predef.???

  // /** @group Updating */
  // val cancelRowUpdates: ResultSetIO[Unit] =
  //   Predef.???

  // /** @group Warnings */
  // val clearWarnings: ResultSetIO[Unit] =
  //   Predef.???

  // /** @group Updating */
  // val deleteRow: ResultSetIO[Unit] =
  //   Predef.???

  // /** @group Cursor Control */
  // def findColumn(a: String): ResultSetIO[Int] =
  //   Predef.???

  // /** @group Cursor Control */
  // val first: ResultSetIO[Boolean] =
  //   Predef.???

  /** @group Results */
  def get[A](n: Int)(implicit A: Composite[A]): ResultSetIO[A] =
    A.get(n)

  /** @group Results */
  def get[A](implicit A: Composite[A]): ResultSetIO[A] =
    A.get(1)

  /** @group Results */
  def getNext[A: Composite]: ResultSetIO[Option[A]] =
    next >>= {
      case true  => get[A].map(Some(_))
      case false => Monad[ResultSetIO].point(None)
    }
    
  /** @group Results */
  def getUnique[A: Composite]: ResultSetIO[A] =
    (getNext[A] |@| next) {
      case (Some(a), false) => a
      case (Some(a), true)  => throw UnexpectedContinuation
      case (None, _)        => throw UnexpectedEnd
    }

  /** @group Results */
  def process[A: Composite]: Process[ResultSetIO, A] = 
    Process.repeatEval(getNext[A]).takeWhile(_.isDefined).map(_.get)

  // /** Consume all remaining by passing to effectful action `effect`. */
  // def sink[A: Composite](effect: A => IO[Unit]): ResultSetIO[Unit] = 
  //   resultset.push(s"sink($effect)")(process[A].to(mkSink(effect)).run)

  // /** @group Properties */
  // val getFetchDirection: ResultSetIO[Int] =
  //   Predef.???

  // /** @group Properties */
  // val getFetchSize: ResultSetIO[Int] =
  //   Predef.???

  // /** @group Properties */
  // val getHoldability: ResultSetIO[Int] =
  //   Predef.???

  // /** @group Properties */
  // val getMetaData: ResultSetIO[ResultSetMetaData] =
  //   Predef.???

  // /** @group Cursor Control */
  // val getRow: ResultSetIO[Int] =
  //   Predef.???

  // /** @group Warnings */
  // val getWarnings: ResultSetIO[SQLWarning] =
  //   Predef.???

  // /** @group Updating */
  // val insertRow: ResultSetIO[Unit] =
  //   Predef.???

  // /** @group Cursor Control */
  // val isAfterLast: ResultSetIO[Boolean] =
  //   Predef.???

  // /** @group Cursor Control */
  // val isBeforeFirst: ResultSetIO[Boolean] =
  //   Predef.???

  // /** @group Cursor Control */
  // val isFirst: ResultSetIO[Boolean] =
  //   Predef.???

  // /** @group Cursor Control */
  // val isLast: ResultSetIO[Boolean] =
  //   Predef.???

  // /** @group Cursor Control */
  // val last: ResultSetIO[Boolean] =
  //   Predef.???

  // /** @group Cursor Control */
  // val moveToCurrentRow: ResultSetIO[Unit] =
  //   Predef.???

  // /** @group Cursor Control */
  // val moveToInsertRow: ResultSetIO[Unit] =
  //   Predef.???

  /** @group Cursor Control */
  val next: ResultSetIO[Boolean] =
    RS.next

  // /** @group Cursor Control */
  // val previous: ResultSetIO[Boolean] =
  //   Predef.???

  // /** @group Cursor Control */
  // val refreshRow: ResultSetIO[Unit] =
  //   Predef.???

  // /** @group Cursor Control */
  // def relative(a: Int): ResultSetIO[Boolean] =
  //   Predef.???

  // /** @group Cursor Control */
  // val rowDeleted: ResultSetIO[Boolean] =
  //   Predef.???

  // /** @group Cursor Control */
  // val rowInserted: ResultSetIO[Boolean] =
  //   Predef.???

  // /** @group Cursor Control */
  // val rowUpdated: ResultSetIO[Boolean] =
  //   Predef.???

  // /** @group Properties */
  // def setFetchDirection(a: Int): ResultSetIO[Unit] =
  //   Predef.???

  // /** @group Properties */
  // def setFetchSize(a: Int): ResultSetIO[Unit] =
  //   Predef.???

}