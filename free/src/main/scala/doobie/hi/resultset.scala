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

import java.net.URL
import java.util.{ Date, Calendar }
import java.sql.{ ParameterMetaData, SQLWarning, Time, Timestamp, Ref, RowId }

import scala.collection.immutable.Map
import scala.collection.JavaConverters._
import scala.Predef.intArrayOps

import scalaz.syntax.id._

/**
 * Module of high-level constructors for `ResultSetIO` actions.
 * @group Modules
 */
object resultset {

  /** @group Typeclass Instances */
  implicit val MonadResultSetIO = RS.MonadResultSetIO

  /** @group Typeclass Instances */
  implicit val CatchableResultSetIO = RS.CatchableResultSetIO

  val wasNull: ResultSetIO[Boolean] =
    RS.wasNull

  val next: ResultSetIO[Boolean] =
    RS.next

  def getString(n:Int): ResultSetIO[String] =
    RS.getString(n)

}