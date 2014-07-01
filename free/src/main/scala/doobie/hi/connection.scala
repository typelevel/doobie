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

import java.sql.Savepoint

import scala.collection.immutable.Map
import scala.collection.JavaConverters._

/**
 * Module of high-level constructors for `ConnectionIO` actions. The constructors here are defined
 * in terms of those in `doobie.free.connection` but differ in the following ways:
 *
 *  - Enumerated values represented by `Int` values in JDBC are mapped to one of the proper types
 *    defined in `doobie.enum`.
 *  - Java collection types are translated to immutable Scala equivalents.
 *  - Actions that compute liftime-managed resources do not return the resource directly, but rather 
 *    take a continuation in the resource's monad.
 *  - Actions that compute values of impure types (`CLOB`, `InputStream`, etc.) do not appear in this API.
 *    They are available in the low-level API but must be used with considerable caution.
 *  - An exception to the above rule is that actions consuming or returning Scala `Array` are available
 *    here but use `scalaz.ImmutableArray` or `List`, depending on usage.
 *  - Lifting actions, low-level type mapping actions, and resource management actions do not appear 
 *    in this API.
 *
 * @group Modules
 */
object connection {

  /** @group Transaction Control */
  val commit: ConnectionIO[Unit] =
    C.commit

  /** 
   * @group Statements
   */
  def createStatement[A](k: StatementIO[A]): ConnectionIO[A] =
    C.createStatement.flatMap(s => C.liftStatement(s, k ensuring S.close))

  /** 
   * @group Statements
   */
  def createStatement[A](rst: Int, rsc: Int)(k: StatementIO[A]): ConnectionIO[A] =
    C.createStatement(rst, rsc).flatMap(s => C.liftStatement(s, k ensuring S.close))

  /** 
   * @group Statements
   */
  def createStatement[A](rst: Int, rsc: Int, rsh: Holdability)(k: StatementIO[A]): ConnectionIO[A] =
    C.createStatement(rst, rsc, rsh.toInt).flatMap(s => C.liftStatement(s, k ensuring S.close))

  /** 
   * @group Connection Properties
   */
  val getCatalog: ConnectionIO[String] =
    C.getCatalog

  /** 
   * @group Connection Properties
   */
  def getClientInfo(key: String): ConnectionIO[Option[String]] =
    C.getClientInfo(key).map(Option(_))

  /** 
   * @group Connection Properties
   */
  val getClientInfo: ConnectionIO[Map[String, String]] =
    C.getClientInfo.map(_.asScala.toMap)

  /** 
   * @group Connection Properties
   */
  val getHoldability: ConnectionIO[Holdability] =
    C.getHoldability.map(Holdability.unsafeFromInt)

  /** 
   * @group Connection Properties
   */
  def getMetaData[A](k: DatabaseMetaDataIO[A]): ConnectionIO[A] =
    C.getMetaData.flatMap(s => C.liftDatabaseMetaData(s, k))

  /** 
   * @group Transaction Control
   */
  val getTransactionIsolation: ConnectionIO[TransactionIsolation] =
    C.getTransactionIsolation.map(TransactionIsolation.unsafeFromInt)

  /** 
   * @group Connection Properties
   */
  val isReadOnly: ConnectionIO[Boolean] =
    C.isReadOnly

  /** 
   * @group Callable Statements
   */
  def prepareCall[A](sql: String, b: Int, c: Int)(k: CallableStatementIO[A]): ConnectionIO[A] =
    C.prepareCall(sql, b, c).flatMap(s => C.liftCallableStatement(s, k ensuring CS.close))

  /** 
   * @group Callable Statements
   */
  def prepareCall[A](sql: String)(k: CallableStatementIO[A]): ConnectionIO[A] =
    C.prepareCall(sql).flatMap(s => C.liftCallableStatement(s, k ensuring CS.close))

  /** 
   * @group Callable Statements
   */
  def prepareCall[A](sql: String, b: Int, c: Int, d: Int)(k: CallableStatementIO[A]): ConnectionIO[A] =
    C.prepareCall(sql, b, c,d).flatMap(s => C.liftCallableStatement(s, k ensuring CS.close))

  /** 
   * @group Prepared Statements
   */
  def prepareStatement[A](sql: String, b: Int, c: Int)(k: PreparedStatementIO[A]): ConnectionIO[A] =
    C.prepareStatement(sql, b, c).flatMap(s => C.liftPreparedStatement(s, k ensuring PS.close))

  /** 
   * @group Prepared Statements
   */
  def prepareStatement[A](sql: String)(k: PreparedStatementIO[A]): ConnectionIO[A] =
    C.prepareStatement(sql).flatMap(s => C.liftPreparedStatement(s, k ensuring PS.close))

  /** 
   * @group Prepared Statements
   */
  def prepareStatement[A](sql: String, b: Int, c: Int, d: Int)(k: PreparedStatementIO[A]): ConnectionIO[A] =
    C.prepareStatement(sql, b, c, d).flatMap(s => C.liftPreparedStatement(s, k ensuring PS.close))

  /** 
   * @group Prepared Statements
   */
  def prepareStatement[A](sql: String, b: Int)(k: PreparedStatementIO[A]): ConnectionIO[A] =
    C.prepareStatement(sql, b).flatMap(s => C.liftPreparedStatement(s, k ensuring PS.close))

  /** 
   * @group Prepared Statements
   */
  def prepareStatementI[A](sql: String, b: List[Int])(k: PreparedStatementIO[A]): ConnectionIO[A] =
    C.prepareStatement(sql, b.toArray).flatMap(s => C.liftPreparedStatement(s, k ensuring PS.close))

  /** 
   * @group Prepared Statements
   */
  def prepareStatementS[A](sql: String, b: List[String])(k: PreparedStatementIO[A]): ConnectionIO[A] =
    C.prepareStatement(sql, b.toArray).flatMap(s => C.liftPreparedStatement(s, k ensuring PS.close))

  /** 
   * @group Transaction Control
   */
  def releaseSavepoint(sp: Savepoint): ConnectionIO[Unit] =
    C.releaseSavepoint(sp)

  /** 
   * @group Transaction Control
   */
  def rollback(sp: Savepoint): ConnectionIO[Unit] =
    C.rollback(sp)

  /** 
   * @group Transaction Control
   */
  val rollback: ConnectionIO[Unit] =
    C.rollback

  /** 
   * @group Connection Properties
   */
  def setCatalog(catalog: String): ConnectionIO[Unit] =
    C.setCatalog(catalog)

  /** 
   * @group Connection Properties
   */
  def setClientInfo(key: String, value: String): ConnectionIO[Unit] =
    C.setClientInfo(key, value)

  /** 
   * @group Connection Properties
   */
  def setClientInfo(info: Map[String, String]): ConnectionIO[Unit] =
    C.setClientInfo { 
      val ps = new java.util.Properties
      ps.putAll(info.asJava)
      ps
    }

  /** 
   * @group Connection Properties
   */
  def setHoldability(h: Holdability): ConnectionIO[Unit] =
    C.setHoldability(h.toInt)

  /** 
   * @group Connection Properties
   */
  def setReadOnly(readOnly: Boolean): ConnectionIO[Unit] =
    C.setReadOnly(readOnly)

  /** 
   * @group Transaction Control
   */
  val setSavepoint: ConnectionIO[Savepoint] =
    C.setSavepoint

  /** 
   * @group Transaction Control
   */
  def setSavepoint(name: String): ConnectionIO[Savepoint] =
    C.setSavepoint(name)

  /** 
   * @group Transaction Control
   */
  def setTransactionIsolation(ti: TransactionIsolation): ConnectionIO[Unit] =
    C.setTransactionIsolation(ti.toInt)


}