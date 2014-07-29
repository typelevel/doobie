package doobie.free

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._

import java.lang.Class
import java.lang.Object
import java.lang.String
import java.sql.Blob
import java.sql.CallableStatement
import java.sql.Clob
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.Driver
import java.sql.NClob
import java.sql.PreparedStatement
import java.sql.Ref
import java.sql.ResultSet
import java.sql.RowIdLifetime
import java.sql.SQLData
import java.sql.SQLInput
import java.sql.SQLOutput
import java.sql.Statement

import nclob.NClobIO
import blob.BlobIO
import clob.ClobIO
import databasemetadata.DatabaseMetaDataIO
import driver.DriverIO
import ref.RefIO
import sqldata.SQLDataIO
import sqlinput.SQLInputIO
import sqloutput.SQLOutputIO
import connection.ConnectionIO
import statement.StatementIO
import preparedstatement.PreparedStatementIO
import callablestatement.CallableStatementIO
import resultset.ResultSetIO

/**
 * Algebra and free monad for primitive operations over a `java.sql.DatabaseMetaData`. This is
 * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly 
 * for library developers. End users will prefer a safer, higher-level API such as that provided 
 * in the `doobie.hi` package.
 *
 * `DatabaseMetaDataIO` is a free monad that must be run via an interpreter, most commonly via
 * natural transformation of its underlying algebra `DatabaseMetaDataOp` to another monad via
 * `Free.runFC`. 
 *
 * The library provides a natural transformation to `Kleisli[M, DatabaseMetaData, A]` for any
 * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is 
 * provided for `Task`, `IO`, and stdlib `Future`; and `liftK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: DatabaseMetaDataIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: DatabaseMetaData = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.liftK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object databasemetadata {
  
  /** 
   * Sum type of primitive operations over a `java.sql.DatabaseMetaData`.
   * @group Algebra 
   */
  sealed trait DatabaseMetaDataOp[A]

  /** 
   * Module of constructors for `DatabaseMetaDataOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `databasemetadata` module.
   * @group Algebra 
   */
  object DatabaseMetaDataOp {
    
    // Lifting
    case class LiftBlobIO[A](s: Blob, action: BlobIO[A]) extends DatabaseMetaDataOp[A]
    case class LiftCallableStatementIO[A](s: CallableStatement, action: CallableStatementIO[A]) extends DatabaseMetaDataOp[A]
    case class LiftClobIO[A](s: Clob, action: ClobIO[A]) extends DatabaseMetaDataOp[A]
    case class LiftConnectionIO[A](s: Connection, action: ConnectionIO[A]) extends DatabaseMetaDataOp[A]
    case class LiftDriverIO[A](s: Driver, action: DriverIO[A]) extends DatabaseMetaDataOp[A]
    case class LiftNClobIO[A](s: NClob, action: NClobIO[A]) extends DatabaseMetaDataOp[A]
    case class LiftPreparedStatementIO[A](s: PreparedStatement, action: PreparedStatementIO[A]) extends DatabaseMetaDataOp[A]
    case class LiftRefIO[A](s: Ref, action: RefIO[A]) extends DatabaseMetaDataOp[A]
    case class LiftResultSetIO[A](s: ResultSet, action: ResultSetIO[A]) extends DatabaseMetaDataOp[A]
    case class LiftSQLDataIO[A](s: SQLData, action: SQLDataIO[A]) extends DatabaseMetaDataOp[A]
    case class LiftSQLInputIO[A](s: SQLInput, action: SQLInputIO[A]) extends DatabaseMetaDataOp[A]
    case class LiftSQLOutputIO[A](s: SQLOutput, action: SQLOutputIO[A]) extends DatabaseMetaDataOp[A]
    case class LiftStatementIO[A](s: Statement, action: StatementIO[A]) extends DatabaseMetaDataOp[A]

    // Combinators
    case class Attempt[A](action: DatabaseMetaDataIO[A]) extends DatabaseMetaDataOp[Throwable \/ A]
    case class Pure[A](a: () => A) extends DatabaseMetaDataOp[A]

    // Primitive Operations
    case object AllProceduresAreCallable extends DatabaseMetaDataOp[Boolean]
    case object AllTablesAreSelectable extends DatabaseMetaDataOp[Boolean]
    case object AutoCommitFailureClosesAllResultSets extends DatabaseMetaDataOp[Boolean]
    case object DataDefinitionCausesTransactionCommit extends DatabaseMetaDataOp[Boolean]
    case object DataDefinitionIgnoredInTransactions extends DatabaseMetaDataOp[Boolean]
    case class  DeletesAreDetected(a: Int) extends DatabaseMetaDataOp[Boolean]
    case object DoesMaxRowSizeIncludeBlobs extends DatabaseMetaDataOp[Boolean]
    case class  GetAttributes(a: String, b: String, c: String, d: String) extends DatabaseMetaDataOp[ResultSet]
    case class  GetBestRowIdentifier(a: String, b: String, c: String, d: Int, e: Boolean) extends DatabaseMetaDataOp[ResultSet]
    case object GetCatalogSeparator extends DatabaseMetaDataOp[String]
    case object GetCatalogTerm extends DatabaseMetaDataOp[String]
    case object GetCatalogs extends DatabaseMetaDataOp[ResultSet]
    case object GetClientInfoProperties extends DatabaseMetaDataOp[ResultSet]
    case class  GetColumnPrivileges(a: String, b: String, c: String, d: String) extends DatabaseMetaDataOp[ResultSet]
    case class  GetColumns(a: String, b: String, c: String, d: String) extends DatabaseMetaDataOp[ResultSet]
    case object GetConnection extends DatabaseMetaDataOp[Connection]
    case class  GetCrossReference(a: String, b: String, c: String, d: String, e: String, f: String) extends DatabaseMetaDataOp[ResultSet]
    case object GetDatabaseMajorVersion extends DatabaseMetaDataOp[Int]
    case object GetDatabaseMinorVersion extends DatabaseMetaDataOp[Int]
    case object GetDatabaseProductName extends DatabaseMetaDataOp[String]
    case object GetDatabaseProductVersion extends DatabaseMetaDataOp[String]
    case object GetDefaultTransactionIsolation extends DatabaseMetaDataOp[Int]
    case object GetDriverMajorVersion extends DatabaseMetaDataOp[Int]
    case object GetDriverMinorVersion extends DatabaseMetaDataOp[Int]
    case object GetDriverName extends DatabaseMetaDataOp[String]
    case object GetDriverVersion extends DatabaseMetaDataOp[String]
    case class  GetExportedKeys(a: String, b: String, c: String) extends DatabaseMetaDataOp[ResultSet]
    case object GetExtraNameCharacters extends DatabaseMetaDataOp[String]
    case class  GetFunctionColumns(a: String, b: String, c: String, d: String) extends DatabaseMetaDataOp[ResultSet]
    case class  GetFunctions(a: String, b: String, c: String) extends DatabaseMetaDataOp[ResultSet]
    case object GetIdentifierQuoteString extends DatabaseMetaDataOp[String]
    case class  GetImportedKeys(a: String, b: String, c: String) extends DatabaseMetaDataOp[ResultSet]
    case class  GetIndexInfo(a: String, b: String, c: String, d: Boolean, e: Boolean) extends DatabaseMetaDataOp[ResultSet]
    case object GetJDBCMajorVersion extends DatabaseMetaDataOp[Int]
    case object GetJDBCMinorVersion extends DatabaseMetaDataOp[Int]
    case object GetMaxBinaryLiteralLength extends DatabaseMetaDataOp[Int]
    case object GetMaxCatalogNameLength extends DatabaseMetaDataOp[Int]
    case object GetMaxCharLiteralLength extends DatabaseMetaDataOp[Int]
    case object GetMaxColumnNameLength extends DatabaseMetaDataOp[Int]
    case object GetMaxColumnsInGroupBy extends DatabaseMetaDataOp[Int]
    case object GetMaxColumnsInIndex extends DatabaseMetaDataOp[Int]
    case object GetMaxColumnsInOrderBy extends DatabaseMetaDataOp[Int]
    case object GetMaxColumnsInSelect extends DatabaseMetaDataOp[Int]
    case object GetMaxColumnsInTable extends DatabaseMetaDataOp[Int]
    case object GetMaxConnections extends DatabaseMetaDataOp[Int]
    case object GetMaxCursorNameLength extends DatabaseMetaDataOp[Int]
    case object GetMaxIndexLength extends DatabaseMetaDataOp[Int]
    case object GetMaxProcedureNameLength extends DatabaseMetaDataOp[Int]
    case object GetMaxRowSize extends DatabaseMetaDataOp[Int]
    case object GetMaxSchemaNameLength extends DatabaseMetaDataOp[Int]
    case object GetMaxStatementLength extends DatabaseMetaDataOp[Int]
    case object GetMaxStatements extends DatabaseMetaDataOp[Int]
    case object GetMaxTableNameLength extends DatabaseMetaDataOp[Int]
    case object GetMaxTablesInSelect extends DatabaseMetaDataOp[Int]
    case object GetMaxUserNameLength extends DatabaseMetaDataOp[Int]
    case object GetNumericFunctions extends DatabaseMetaDataOp[String]
    case class  GetPrimaryKeys(a: String, b: String, c: String) extends DatabaseMetaDataOp[ResultSet]
    case class  GetProcedureColumns(a: String, b: String, c: String, d: String) extends DatabaseMetaDataOp[ResultSet]
    case object GetProcedureTerm extends DatabaseMetaDataOp[String]
    case class  GetProcedures(a: String, b: String, c: String) extends DatabaseMetaDataOp[ResultSet]
    case object GetResultSetHoldability extends DatabaseMetaDataOp[Int]
    case object GetRowIdLifetime extends DatabaseMetaDataOp[RowIdLifetime]
    case object GetSQLKeywords extends DatabaseMetaDataOp[String]
    case object GetSQLStateType extends DatabaseMetaDataOp[Int]
    case object GetSchemaTerm extends DatabaseMetaDataOp[String]
    case object GetSchemas extends DatabaseMetaDataOp[ResultSet]
    case class  GetSchemas1(a: String, b: String) extends DatabaseMetaDataOp[ResultSet]
    case object GetSearchStringEscape extends DatabaseMetaDataOp[String]
    case object GetStringFunctions extends DatabaseMetaDataOp[String]
    case class  GetSuperTables(a: String, b: String, c: String) extends DatabaseMetaDataOp[ResultSet]
    case class  GetSuperTypes(a: String, b: String, c: String) extends DatabaseMetaDataOp[ResultSet]
    case object GetSystemFunctions extends DatabaseMetaDataOp[String]
    case class  GetTablePrivileges(a: String, b: String, c: String) extends DatabaseMetaDataOp[ResultSet]
    case object GetTableTypes extends DatabaseMetaDataOp[ResultSet]
    case class  GetTables(a: String, b: String, c: String, d: Array[String]) extends DatabaseMetaDataOp[ResultSet]
    case object GetTimeDateFunctions extends DatabaseMetaDataOp[String]
    case object GetTypeInfo extends DatabaseMetaDataOp[ResultSet]
    case class  GetUDTs(a: String, b: String, c: String, d: Array[Int]) extends DatabaseMetaDataOp[ResultSet]
    case object GetURL extends DatabaseMetaDataOp[String]
    case object GetUserName extends DatabaseMetaDataOp[String]
    case class  GetVersionColumns(a: String, b: String, c: String) extends DatabaseMetaDataOp[ResultSet]
    case class  InsertsAreDetected(a: Int) extends DatabaseMetaDataOp[Boolean]
    case object IsCatalogAtStart extends DatabaseMetaDataOp[Boolean]
    case object IsReadOnly extends DatabaseMetaDataOp[Boolean]
    case class  IsWrapperFor(a: Class[_]) extends DatabaseMetaDataOp[Boolean]
    case object LocatorsUpdateCopy extends DatabaseMetaDataOp[Boolean]
    case object NullPlusNonNullIsNull extends DatabaseMetaDataOp[Boolean]
    case object NullsAreSortedAtEnd extends DatabaseMetaDataOp[Boolean]
    case object NullsAreSortedAtStart extends DatabaseMetaDataOp[Boolean]
    case object NullsAreSortedHigh extends DatabaseMetaDataOp[Boolean]
    case object NullsAreSortedLow extends DatabaseMetaDataOp[Boolean]
    case class  OthersDeletesAreVisible(a: Int) extends DatabaseMetaDataOp[Boolean]
    case class  OthersInsertsAreVisible(a: Int) extends DatabaseMetaDataOp[Boolean]
    case class  OthersUpdatesAreVisible(a: Int) extends DatabaseMetaDataOp[Boolean]
    case class  OwnDeletesAreVisible(a: Int) extends DatabaseMetaDataOp[Boolean]
    case class  OwnInsertsAreVisible(a: Int) extends DatabaseMetaDataOp[Boolean]
    case class  OwnUpdatesAreVisible(a: Int) extends DatabaseMetaDataOp[Boolean]
    case object StoresLowerCaseIdentifiers extends DatabaseMetaDataOp[Boolean]
    case object StoresLowerCaseQuotedIdentifiers extends DatabaseMetaDataOp[Boolean]
    case object StoresMixedCaseIdentifiers extends DatabaseMetaDataOp[Boolean]
    case object StoresMixedCaseQuotedIdentifiers extends DatabaseMetaDataOp[Boolean]
    case object StoresUpperCaseIdentifiers extends DatabaseMetaDataOp[Boolean]
    case object StoresUpperCaseQuotedIdentifiers extends DatabaseMetaDataOp[Boolean]
    case object SupportsANSI92EntryLevelSQL extends DatabaseMetaDataOp[Boolean]
    case object SupportsANSI92FullSQL extends DatabaseMetaDataOp[Boolean]
    case object SupportsANSI92IntermediateSQL extends DatabaseMetaDataOp[Boolean]
    case object SupportsAlterTableWithAddColumn extends DatabaseMetaDataOp[Boolean]
    case object SupportsAlterTableWithDropColumn extends DatabaseMetaDataOp[Boolean]
    case object SupportsBatchUpdates extends DatabaseMetaDataOp[Boolean]
    case object SupportsCatalogsInDataManipulation extends DatabaseMetaDataOp[Boolean]
    case object SupportsCatalogsInIndexDefinitions extends DatabaseMetaDataOp[Boolean]
    case object SupportsCatalogsInPrivilegeDefinitions extends DatabaseMetaDataOp[Boolean]
    case object SupportsCatalogsInProcedureCalls extends DatabaseMetaDataOp[Boolean]
    case object SupportsCatalogsInTableDefinitions extends DatabaseMetaDataOp[Boolean]
    case object SupportsColumnAliasing extends DatabaseMetaDataOp[Boolean]
    case class  SupportsConvert(a: Int, b: Int) extends DatabaseMetaDataOp[Boolean]
    case object SupportsConvert1 extends DatabaseMetaDataOp[Boolean]
    case object SupportsCoreSQLGrammar extends DatabaseMetaDataOp[Boolean]
    case object SupportsCorrelatedSubqueries extends DatabaseMetaDataOp[Boolean]
    case object SupportsDataDefinitionAndDataManipulationTransactions extends DatabaseMetaDataOp[Boolean]
    case object SupportsDataManipulationTransactionsOnly extends DatabaseMetaDataOp[Boolean]
    case object SupportsDifferentTableCorrelationNames extends DatabaseMetaDataOp[Boolean]
    case object SupportsExpressionsInOrderBy extends DatabaseMetaDataOp[Boolean]
    case object SupportsExtendedSQLGrammar extends DatabaseMetaDataOp[Boolean]
    case object SupportsFullOuterJoins extends DatabaseMetaDataOp[Boolean]
    case object SupportsGetGeneratedKeys extends DatabaseMetaDataOp[Boolean]
    case object SupportsGroupBy extends DatabaseMetaDataOp[Boolean]
    case object SupportsGroupByBeyondSelect extends DatabaseMetaDataOp[Boolean]
    case object SupportsGroupByUnrelated extends DatabaseMetaDataOp[Boolean]
    case object SupportsIntegrityEnhancementFacility extends DatabaseMetaDataOp[Boolean]
    case object SupportsLikeEscapeClause extends DatabaseMetaDataOp[Boolean]
    case object SupportsLimitedOuterJoins extends DatabaseMetaDataOp[Boolean]
    case object SupportsMinimumSQLGrammar extends DatabaseMetaDataOp[Boolean]
    case object SupportsMixedCaseIdentifiers extends DatabaseMetaDataOp[Boolean]
    case object SupportsMixedCaseQuotedIdentifiers extends DatabaseMetaDataOp[Boolean]
    case object SupportsMultipleOpenResults extends DatabaseMetaDataOp[Boolean]
    case object SupportsMultipleResultSets extends DatabaseMetaDataOp[Boolean]
    case object SupportsMultipleTransactions extends DatabaseMetaDataOp[Boolean]
    case object SupportsNamedParameters extends DatabaseMetaDataOp[Boolean]
    case object SupportsNonNullableColumns extends DatabaseMetaDataOp[Boolean]
    case object SupportsOpenCursorsAcrossCommit extends DatabaseMetaDataOp[Boolean]
    case object SupportsOpenCursorsAcrossRollback extends DatabaseMetaDataOp[Boolean]
    case object SupportsOpenStatementsAcrossCommit extends DatabaseMetaDataOp[Boolean]
    case object SupportsOpenStatementsAcrossRollback extends DatabaseMetaDataOp[Boolean]
    case object SupportsOrderByUnrelated extends DatabaseMetaDataOp[Boolean]
    case object SupportsOuterJoins extends DatabaseMetaDataOp[Boolean]
    case object SupportsPositionedDelete extends DatabaseMetaDataOp[Boolean]
    case object SupportsPositionedUpdate extends DatabaseMetaDataOp[Boolean]
    case class  SupportsResultSetConcurrency(a: Int, b: Int) extends DatabaseMetaDataOp[Boolean]
    case class  SupportsResultSetHoldability(a: Int) extends DatabaseMetaDataOp[Boolean]
    case class  SupportsResultSetType(a: Int) extends DatabaseMetaDataOp[Boolean]
    case object SupportsSavepoints extends DatabaseMetaDataOp[Boolean]
    case object SupportsSchemasInDataManipulation extends DatabaseMetaDataOp[Boolean]
    case object SupportsSchemasInIndexDefinitions extends DatabaseMetaDataOp[Boolean]
    case object SupportsSchemasInPrivilegeDefinitions extends DatabaseMetaDataOp[Boolean]
    case object SupportsSchemasInProcedureCalls extends DatabaseMetaDataOp[Boolean]
    case object SupportsSchemasInTableDefinitions extends DatabaseMetaDataOp[Boolean]
    case object SupportsSelectForUpdate extends DatabaseMetaDataOp[Boolean]
    case object SupportsStatementPooling extends DatabaseMetaDataOp[Boolean]
    case object SupportsStoredFunctionsUsingCallSyntax extends DatabaseMetaDataOp[Boolean]
    case object SupportsStoredProcedures extends DatabaseMetaDataOp[Boolean]
    case object SupportsSubqueriesInComparisons extends DatabaseMetaDataOp[Boolean]
    case object SupportsSubqueriesInExists extends DatabaseMetaDataOp[Boolean]
    case object SupportsSubqueriesInIns extends DatabaseMetaDataOp[Boolean]
    case object SupportsSubqueriesInQuantifieds extends DatabaseMetaDataOp[Boolean]
    case object SupportsTableCorrelationNames extends DatabaseMetaDataOp[Boolean]
    case class  SupportsTransactionIsolationLevel(a: Int) extends DatabaseMetaDataOp[Boolean]
    case object SupportsTransactions extends DatabaseMetaDataOp[Boolean]
    case object SupportsUnion extends DatabaseMetaDataOp[Boolean]
    case object SupportsUnionAll extends DatabaseMetaDataOp[Boolean]
    case class  Unwrap[T](a: Class[T]) extends DatabaseMetaDataOp[T]
    case class  UpdatesAreDetected(a: Int) extends DatabaseMetaDataOp[Boolean]
    case object UsesLocalFilePerTable extends DatabaseMetaDataOp[Boolean]
    case object UsesLocalFiles extends DatabaseMetaDataOp[Boolean]

  }
  import DatabaseMetaDataOp._ // We use these immediately

  /**
   * Free monad over a free functor of [[DatabaseMetaDataOp]]; abstractly, a computation that consumes 
   * a `java.sql.DatabaseMetaData` and produces a value of type `A`. 
   * @group Algebra 
   */
  type DatabaseMetaDataIO[A] = F.FreeC[DatabaseMetaDataOp, A]

  /**
   * Monad instance for [[DatabaseMetaDataIO]] (can't be inferred).
   * @group Typeclass Instances 
   */
  implicit val MonadDatabaseMetaDataIO: Monad[DatabaseMetaDataIO] = 
    F.freeMonad[({type λ[α] = Coyoneda[DatabaseMetaDataOp, α]})#λ]

  /**
   * Catchable instance for [[DatabaseMetaDataIO]].
   * @group Typeclass Instances
   */
  implicit val CatchableDatabaseMetaDataIO: Catchable[DatabaseMetaDataIO] =
    new Catchable[DatabaseMetaDataIO] {
      def attempt[A](f: DatabaseMetaDataIO[A]): DatabaseMetaDataIO[Throwable \/ A] = databasemetadata.attempt(f)
      def fail[A](err: Throwable): DatabaseMetaDataIO[A] = databasemetadata.delay(throw err)
    }

  /**
   * @group Constructors (Lifting)
   */
  def liftBlob[A](s: Blob, k: BlobIO[A]): DatabaseMetaDataIO[A] =
    F.liftFC(LiftBlobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftCallableStatement[A](s: CallableStatement, k: CallableStatementIO[A]): DatabaseMetaDataIO[A] =
    F.liftFC(LiftCallableStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftClob[A](s: Clob, k: ClobIO[A]): DatabaseMetaDataIO[A] =
    F.liftFC(LiftClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftConnection[A](s: Connection, k: ConnectionIO[A]): DatabaseMetaDataIO[A] =
    F.liftFC(LiftConnectionIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftDriver[A](s: Driver, k: DriverIO[A]): DatabaseMetaDataIO[A] =
    F.liftFC(LiftDriverIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftNClob[A](s: NClob, k: NClobIO[A]): DatabaseMetaDataIO[A] =
    F.liftFC(LiftNClobIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftPreparedStatement[A](s: PreparedStatement, k: PreparedStatementIO[A]): DatabaseMetaDataIO[A] =
    F.liftFC(LiftPreparedStatementIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftRef[A](s: Ref, k: RefIO[A]): DatabaseMetaDataIO[A] =
    F.liftFC(LiftRefIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftResultSet[A](s: ResultSet, k: ResultSetIO[A]): DatabaseMetaDataIO[A] =
    F.liftFC(LiftResultSetIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLData[A](s: SQLData, k: SQLDataIO[A]): DatabaseMetaDataIO[A] =
    F.liftFC(LiftSQLDataIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLInput[A](s: SQLInput, k: SQLInputIO[A]): DatabaseMetaDataIO[A] =
    F.liftFC(LiftSQLInputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftSQLOutput[A](s: SQLOutput, k: SQLOutputIO[A]): DatabaseMetaDataIO[A] =
    F.liftFC(LiftSQLOutputIO(s, k))

  /**
   * @group Constructors (Lifting)
   */
  def liftStatement[A](s: Statement, k: StatementIO[A]): DatabaseMetaDataIO[A] =
    F.liftFC(LiftStatementIO(s, k))

  /** 
   * Lift a DatabaseMetaDataIO[A] into an exception-capturing DatabaseMetaDataIO[Throwable \/ A].
   * @group Constructors (Lifting)
   */
  def attempt[A](a: DatabaseMetaDataIO[A]): DatabaseMetaDataIO[Throwable \/ A] =
    F.liftFC[DatabaseMetaDataOp, Throwable \/ A](Attempt(a))
 
  /**
   * Non-strict unit for capturing effects.
   * @group Constructors (Lifting)
   */
  def delay[A](a: => A): DatabaseMetaDataIO[A] =
    F.liftFC(Pure(a _))

  /** 
   * @group Constructors (Primitives)
   */
  val allProceduresAreCallable: DatabaseMetaDataIO[Boolean] =
    F.liftFC(AllProceduresAreCallable)

  /** 
   * @group Constructors (Primitives)
   */
  val allTablesAreSelectable: DatabaseMetaDataIO[Boolean] =
    F.liftFC(AllTablesAreSelectable)

  /** 
   * @group Constructors (Primitives)
   */
  val autoCommitFailureClosesAllResultSets: DatabaseMetaDataIO[Boolean] =
    F.liftFC(AutoCommitFailureClosesAllResultSets)

  /** 
   * @group Constructors (Primitives)
   */
  val dataDefinitionCausesTransactionCommit: DatabaseMetaDataIO[Boolean] =
    F.liftFC(DataDefinitionCausesTransactionCommit)

  /** 
   * @group Constructors (Primitives)
   */
  val dataDefinitionIgnoredInTransactions: DatabaseMetaDataIO[Boolean] =
    F.liftFC(DataDefinitionIgnoredInTransactions)

  /** 
   * @group Constructors (Primitives)
   */
  def deletesAreDetected(a: Int): DatabaseMetaDataIO[Boolean] =
    F.liftFC(DeletesAreDetected(a))

  /** 
   * @group Constructors (Primitives)
   */
  val doesMaxRowSizeIncludeBlobs: DatabaseMetaDataIO[Boolean] =
    F.liftFC(DoesMaxRowSizeIncludeBlobs)

  /** 
   * @group Constructors (Primitives)
   */
  def getAttributes(a: String, b: String, c: String, d: String): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetAttributes(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  def getBestRowIdentifier(a: String, b: String, c: String, d: Int, e: Boolean): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetBestRowIdentifier(a, b, c, d, e))

  /** 
   * @group Constructors (Primitives)
   */
  val getCatalogSeparator: DatabaseMetaDataIO[String] =
    F.liftFC(GetCatalogSeparator)

  /** 
   * @group Constructors (Primitives)
   */
  val getCatalogTerm: DatabaseMetaDataIO[String] =
    F.liftFC(GetCatalogTerm)

  /** 
   * @group Constructors (Primitives)
   */
  val getCatalogs: DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetCatalogs)

  /** 
   * @group Constructors (Primitives)
   */
  val getClientInfoProperties: DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetClientInfoProperties)

  /** 
   * @group Constructors (Primitives)
   */
  def getColumnPrivileges(a: String, b: String, c: String, d: String): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetColumnPrivileges(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  def getColumns(a: String, b: String, c: String, d: String): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetColumns(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  val getConnection: DatabaseMetaDataIO[Connection] =
    F.liftFC(GetConnection)

  /** 
   * @group Constructors (Primitives)
   */
  def getCrossReference(a: String, b: String, c: String, d: String, e: String, f: String): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetCrossReference(a, b, c, d, e, f))

  /** 
   * @group Constructors (Primitives)
   */
  val getDatabaseMajorVersion: DatabaseMetaDataIO[Int] =
    F.liftFC(GetDatabaseMajorVersion)

  /** 
   * @group Constructors (Primitives)
   */
  val getDatabaseMinorVersion: DatabaseMetaDataIO[Int] =
    F.liftFC(GetDatabaseMinorVersion)

  /** 
   * @group Constructors (Primitives)
   */
  val getDatabaseProductName: DatabaseMetaDataIO[String] =
    F.liftFC(GetDatabaseProductName)

  /** 
   * @group Constructors (Primitives)
   */
  val getDatabaseProductVersion: DatabaseMetaDataIO[String] =
    F.liftFC(GetDatabaseProductVersion)

  /** 
   * @group Constructors (Primitives)
   */
  val getDefaultTransactionIsolation: DatabaseMetaDataIO[Int] =
    F.liftFC(GetDefaultTransactionIsolation)

  /** 
   * @group Constructors (Primitives)
   */
  val getDriverMajorVersion: DatabaseMetaDataIO[Int] =
    F.liftFC(GetDriverMajorVersion)

  /** 
   * @group Constructors (Primitives)
   */
  val getDriverMinorVersion: DatabaseMetaDataIO[Int] =
    F.liftFC(GetDriverMinorVersion)

  /** 
   * @group Constructors (Primitives)
   */
  val getDriverName: DatabaseMetaDataIO[String] =
    F.liftFC(GetDriverName)

  /** 
   * @group Constructors (Primitives)
   */
  val getDriverVersion: DatabaseMetaDataIO[String] =
    F.liftFC(GetDriverVersion)

  /** 
   * @group Constructors (Primitives)
   */
  def getExportedKeys(a: String, b: String, c: String): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetExportedKeys(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  val getExtraNameCharacters: DatabaseMetaDataIO[String] =
    F.liftFC(GetExtraNameCharacters)

  /** 
   * @group Constructors (Primitives)
   */
  def getFunctionColumns(a: String, b: String, c: String, d: String): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetFunctionColumns(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  def getFunctions(a: String, b: String, c: String): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetFunctions(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  val getIdentifierQuoteString: DatabaseMetaDataIO[String] =
    F.liftFC(GetIdentifierQuoteString)

  /** 
   * @group Constructors (Primitives)
   */
  def getImportedKeys(a: String, b: String, c: String): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetImportedKeys(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def getIndexInfo(a: String, b: String, c: String, d: Boolean, e: Boolean): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetIndexInfo(a, b, c, d, e))

  /** 
   * @group Constructors (Primitives)
   */
  val getJDBCMajorVersion: DatabaseMetaDataIO[Int] =
    F.liftFC(GetJDBCMajorVersion)

  /** 
   * @group Constructors (Primitives)
   */
  val getJDBCMinorVersion: DatabaseMetaDataIO[Int] =
    F.liftFC(GetJDBCMinorVersion)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxBinaryLiteralLength: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxBinaryLiteralLength)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxCatalogNameLength: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxCatalogNameLength)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxCharLiteralLength: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxCharLiteralLength)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxColumnNameLength: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxColumnNameLength)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxColumnsInGroupBy: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxColumnsInGroupBy)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxColumnsInIndex: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxColumnsInIndex)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxColumnsInOrderBy: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxColumnsInOrderBy)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxColumnsInSelect: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxColumnsInSelect)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxColumnsInTable: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxColumnsInTable)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxConnections: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxConnections)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxCursorNameLength: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxCursorNameLength)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxIndexLength: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxIndexLength)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxProcedureNameLength: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxProcedureNameLength)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxRowSize: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxRowSize)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxSchemaNameLength: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxSchemaNameLength)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxStatementLength: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxStatementLength)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxStatements: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxStatements)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxTableNameLength: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxTableNameLength)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxTablesInSelect: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxTablesInSelect)

  /** 
   * @group Constructors (Primitives)
   */
  val getMaxUserNameLength: DatabaseMetaDataIO[Int] =
    F.liftFC(GetMaxUserNameLength)

  /** 
   * @group Constructors (Primitives)
   */
  val getNumericFunctions: DatabaseMetaDataIO[String] =
    F.liftFC(GetNumericFunctions)

  /** 
   * @group Constructors (Primitives)
   */
  def getPrimaryKeys(a: String, b: String, c: String): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetPrimaryKeys(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def getProcedureColumns(a: String, b: String, c: String, d: String): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetProcedureColumns(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  val getProcedureTerm: DatabaseMetaDataIO[String] =
    F.liftFC(GetProcedureTerm)

  /** 
   * @group Constructors (Primitives)
   */
  def getProcedures(a: String, b: String, c: String): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetProcedures(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  val getResultSetHoldability: DatabaseMetaDataIO[Int] =
    F.liftFC(GetResultSetHoldability)

  /** 
   * @group Constructors (Primitives)
   */
  val getRowIdLifetime: DatabaseMetaDataIO[RowIdLifetime] =
    F.liftFC(GetRowIdLifetime)

  /** 
   * @group Constructors (Primitives)
   */
  val getSQLKeywords: DatabaseMetaDataIO[String] =
    F.liftFC(GetSQLKeywords)

  /** 
   * @group Constructors (Primitives)
   */
  val getSQLStateType: DatabaseMetaDataIO[Int] =
    F.liftFC(GetSQLStateType)

  /** 
   * @group Constructors (Primitives)
   */
  val getSchemaTerm: DatabaseMetaDataIO[String] =
    F.liftFC(GetSchemaTerm)

  /** 
   * @group Constructors (Primitives)
   */
  val getSchemas: DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetSchemas)

  /** 
   * @group Constructors (Primitives)
   */
  def getSchemas(a: String, b: String): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetSchemas1(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val getSearchStringEscape: DatabaseMetaDataIO[String] =
    F.liftFC(GetSearchStringEscape)

  /** 
   * @group Constructors (Primitives)
   */
  val getStringFunctions: DatabaseMetaDataIO[String] =
    F.liftFC(GetStringFunctions)

  /** 
   * @group Constructors (Primitives)
   */
  def getSuperTables(a: String, b: String, c: String): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetSuperTables(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def getSuperTypes(a: String, b: String, c: String): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetSuperTypes(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  val getSystemFunctions: DatabaseMetaDataIO[String] =
    F.liftFC(GetSystemFunctions)

  /** 
   * @group Constructors (Primitives)
   */
  def getTablePrivileges(a: String, b: String, c: String): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetTablePrivileges(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  val getTableTypes: DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetTableTypes)

  /** 
   * @group Constructors (Primitives)
   */
  def getTables(a: String, b: String, c: String, d: Array[String]): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetTables(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  val getTimeDateFunctions: DatabaseMetaDataIO[String] =
    F.liftFC(GetTimeDateFunctions)

  /** 
   * @group Constructors (Primitives)
   */
  val getTypeInfo: DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetTypeInfo)

  /** 
   * @group Constructors (Primitives)
   */
  def getUDTs(a: String, b: String, c: String, d: Array[Int]): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetUDTs(a, b, c, d))

  /** 
   * @group Constructors (Primitives)
   */
  val getURL: DatabaseMetaDataIO[String] =
    F.liftFC(GetURL)

  /** 
   * @group Constructors (Primitives)
   */
  val getUserName: DatabaseMetaDataIO[String] =
    F.liftFC(GetUserName)

  /** 
   * @group Constructors (Primitives)
   */
  def getVersionColumns(a: String, b: String, c: String): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetVersionColumns(a, b, c))

  /** 
   * @group Constructors (Primitives)
   */
  def insertsAreDetected(a: Int): DatabaseMetaDataIO[Boolean] =
    F.liftFC(InsertsAreDetected(a))

  /** 
   * @group Constructors (Primitives)
   */
  val isCatalogAtStart: DatabaseMetaDataIO[Boolean] =
    F.liftFC(IsCatalogAtStart)

  /** 
   * @group Constructors (Primitives)
   */
  val isReadOnly: DatabaseMetaDataIO[Boolean] =
    F.liftFC(IsReadOnly)

  /** 
   * @group Constructors (Primitives)
   */
  def isWrapperFor(a: Class[_]): DatabaseMetaDataIO[Boolean] =
    F.liftFC(IsWrapperFor(a))

  /** 
   * @group Constructors (Primitives)
   */
  val locatorsUpdateCopy: DatabaseMetaDataIO[Boolean] =
    F.liftFC(LocatorsUpdateCopy)

  /** 
   * @group Constructors (Primitives)
   */
  val nullPlusNonNullIsNull: DatabaseMetaDataIO[Boolean] =
    F.liftFC(NullPlusNonNullIsNull)

  /** 
   * @group Constructors (Primitives)
   */
  val nullsAreSortedAtEnd: DatabaseMetaDataIO[Boolean] =
    F.liftFC(NullsAreSortedAtEnd)

  /** 
   * @group Constructors (Primitives)
   */
  val nullsAreSortedAtStart: DatabaseMetaDataIO[Boolean] =
    F.liftFC(NullsAreSortedAtStart)

  /** 
   * @group Constructors (Primitives)
   */
  val nullsAreSortedHigh: DatabaseMetaDataIO[Boolean] =
    F.liftFC(NullsAreSortedHigh)

  /** 
   * @group Constructors (Primitives)
   */
  val nullsAreSortedLow: DatabaseMetaDataIO[Boolean] =
    F.liftFC(NullsAreSortedLow)

  /** 
   * @group Constructors (Primitives)
   */
  def othersDeletesAreVisible(a: Int): DatabaseMetaDataIO[Boolean] =
    F.liftFC(OthersDeletesAreVisible(a))

  /** 
   * @group Constructors (Primitives)
   */
  def othersInsertsAreVisible(a: Int): DatabaseMetaDataIO[Boolean] =
    F.liftFC(OthersInsertsAreVisible(a))

  /** 
   * @group Constructors (Primitives)
   */
  def othersUpdatesAreVisible(a: Int): DatabaseMetaDataIO[Boolean] =
    F.liftFC(OthersUpdatesAreVisible(a))

  /** 
   * @group Constructors (Primitives)
   */
  def ownDeletesAreVisible(a: Int): DatabaseMetaDataIO[Boolean] =
    F.liftFC(OwnDeletesAreVisible(a))

  /** 
   * @group Constructors (Primitives)
   */
  def ownInsertsAreVisible(a: Int): DatabaseMetaDataIO[Boolean] =
    F.liftFC(OwnInsertsAreVisible(a))

  /** 
   * @group Constructors (Primitives)
   */
  def ownUpdatesAreVisible(a: Int): DatabaseMetaDataIO[Boolean] =
    F.liftFC(OwnUpdatesAreVisible(a))

  /** 
   * @group Constructors (Primitives)
   */
  val storesLowerCaseIdentifiers: DatabaseMetaDataIO[Boolean] =
    F.liftFC(StoresLowerCaseIdentifiers)

  /** 
   * @group Constructors (Primitives)
   */
  val storesLowerCaseQuotedIdentifiers: DatabaseMetaDataIO[Boolean] =
    F.liftFC(StoresLowerCaseQuotedIdentifiers)

  /** 
   * @group Constructors (Primitives)
   */
  val storesMixedCaseIdentifiers: DatabaseMetaDataIO[Boolean] =
    F.liftFC(StoresMixedCaseIdentifiers)

  /** 
   * @group Constructors (Primitives)
   */
  val storesMixedCaseQuotedIdentifiers: DatabaseMetaDataIO[Boolean] =
    F.liftFC(StoresMixedCaseQuotedIdentifiers)

  /** 
   * @group Constructors (Primitives)
   */
  val storesUpperCaseIdentifiers: DatabaseMetaDataIO[Boolean] =
    F.liftFC(StoresUpperCaseIdentifiers)

  /** 
   * @group Constructors (Primitives)
   */
  val storesUpperCaseQuotedIdentifiers: DatabaseMetaDataIO[Boolean] =
    F.liftFC(StoresUpperCaseQuotedIdentifiers)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsANSI92EntryLevelSQL: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsANSI92EntryLevelSQL)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsANSI92FullSQL: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsANSI92FullSQL)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsANSI92IntermediateSQL: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsANSI92IntermediateSQL)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsAlterTableWithAddColumn: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsAlterTableWithAddColumn)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsAlterTableWithDropColumn: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsAlterTableWithDropColumn)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsBatchUpdates: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsBatchUpdates)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsCatalogsInDataManipulation: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsCatalogsInDataManipulation)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsCatalogsInIndexDefinitions: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsCatalogsInIndexDefinitions)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsCatalogsInPrivilegeDefinitions: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsCatalogsInPrivilegeDefinitions)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsCatalogsInProcedureCalls: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsCatalogsInProcedureCalls)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsCatalogsInTableDefinitions: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsCatalogsInTableDefinitions)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsColumnAliasing: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsColumnAliasing)

  /** 
   * @group Constructors (Primitives)
   */
  def supportsConvert(a: Int, b: Int): DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsConvert(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val supportsConvert: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsConvert1)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsCoreSQLGrammar: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsCoreSQLGrammar)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsCorrelatedSubqueries: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsCorrelatedSubqueries)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsDataDefinitionAndDataManipulationTransactions: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsDataDefinitionAndDataManipulationTransactions)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsDataManipulationTransactionsOnly: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsDataManipulationTransactionsOnly)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsDifferentTableCorrelationNames: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsDifferentTableCorrelationNames)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsExpressionsInOrderBy: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsExpressionsInOrderBy)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsExtendedSQLGrammar: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsExtendedSQLGrammar)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsFullOuterJoins: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsFullOuterJoins)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsGetGeneratedKeys: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsGetGeneratedKeys)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsGroupBy: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsGroupBy)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsGroupByBeyondSelect: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsGroupByBeyondSelect)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsGroupByUnrelated: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsGroupByUnrelated)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsIntegrityEnhancementFacility: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsIntegrityEnhancementFacility)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsLikeEscapeClause: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsLikeEscapeClause)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsLimitedOuterJoins: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsLimitedOuterJoins)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsMinimumSQLGrammar: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsMinimumSQLGrammar)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsMixedCaseIdentifiers: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsMixedCaseIdentifiers)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsMixedCaseQuotedIdentifiers: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsMixedCaseQuotedIdentifiers)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsMultipleOpenResults: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsMultipleOpenResults)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsMultipleResultSets: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsMultipleResultSets)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsMultipleTransactions: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsMultipleTransactions)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsNamedParameters: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsNamedParameters)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsNonNullableColumns: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsNonNullableColumns)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsOpenCursorsAcrossCommit: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsOpenCursorsAcrossCommit)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsOpenCursorsAcrossRollback: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsOpenCursorsAcrossRollback)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsOpenStatementsAcrossCommit: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsOpenStatementsAcrossCommit)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsOpenStatementsAcrossRollback: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsOpenStatementsAcrossRollback)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsOrderByUnrelated: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsOrderByUnrelated)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsOuterJoins: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsOuterJoins)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsPositionedDelete: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsPositionedDelete)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsPositionedUpdate: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsPositionedUpdate)

  /** 
   * @group Constructors (Primitives)
   */
  def supportsResultSetConcurrency(a: Int, b: Int): DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsResultSetConcurrency(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  def supportsResultSetHoldability(a: Int): DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsResultSetHoldability(a))

  /** 
   * @group Constructors (Primitives)
   */
  def supportsResultSetType(a: Int): DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsResultSetType(a))

  /** 
   * @group Constructors (Primitives)
   */
  val supportsSavepoints: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsSavepoints)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsSchemasInDataManipulation: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsSchemasInDataManipulation)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsSchemasInIndexDefinitions: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsSchemasInIndexDefinitions)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsSchemasInPrivilegeDefinitions: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsSchemasInPrivilegeDefinitions)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsSchemasInProcedureCalls: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsSchemasInProcedureCalls)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsSchemasInTableDefinitions: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsSchemasInTableDefinitions)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsSelectForUpdate: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsSelectForUpdate)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsStatementPooling: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsStatementPooling)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsStoredFunctionsUsingCallSyntax: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsStoredFunctionsUsingCallSyntax)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsStoredProcedures: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsStoredProcedures)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsSubqueriesInComparisons: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsSubqueriesInComparisons)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsSubqueriesInExists: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsSubqueriesInExists)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsSubqueriesInIns: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsSubqueriesInIns)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsSubqueriesInQuantifieds: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsSubqueriesInQuantifieds)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsTableCorrelationNames: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsTableCorrelationNames)

  /** 
   * @group Constructors (Primitives)
   */
  def supportsTransactionIsolationLevel(a: Int): DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsTransactionIsolationLevel(a))

  /** 
   * @group Constructors (Primitives)
   */
  val supportsTransactions: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsTransactions)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsUnion: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsUnion)

  /** 
   * @group Constructors (Primitives)
   */
  val supportsUnionAll: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsUnionAll)

  /** 
   * @group Constructors (Primitives)
   */
  def unwrap[T](a: Class[T]): DatabaseMetaDataIO[T] =
    F.liftFC(Unwrap(a))

  /** 
   * @group Constructors (Primitives)
   */
  def updatesAreDetected(a: Int): DatabaseMetaDataIO[Boolean] =
    F.liftFC(UpdatesAreDetected(a))

  /** 
   * @group Constructors (Primitives)
   */
  val usesLocalFilePerTable: DatabaseMetaDataIO[Boolean] =
    F.liftFC(UsesLocalFilePerTable)

  /** 
   * @group Constructors (Primitives)
   */
  val usesLocalFiles: DatabaseMetaDataIO[Boolean] =
    F.liftFC(UsesLocalFiles)

 /** 
  * Natural transformation from `DatabaseMetaDataOp` to `Kleisli` for the given `M`, consuming a `java.sql.DatabaseMetaData`. 
  * @group Algebra
  */
 def kleisliTrans[M[_]: Monad: Catchable: Capture]: DatabaseMetaDataOp ~> ({type l[a] = Kleisli[M, DatabaseMetaData, a]})#l =
   new (DatabaseMetaDataOp ~> ({type l[a] = Kleisli[M, DatabaseMetaData, a]})#l) {
     import scalaz.syntax.catchable._

     val L = Predef.implicitly[Capture[M]]

     def primitive[A](f: DatabaseMetaData => A): Kleisli[M, DatabaseMetaData, A] =
       Kleisli(s => L.apply(f(s)))

     def apply[A](op: DatabaseMetaDataOp[A]): Kleisli[M, DatabaseMetaData, A] = 
       op match {

        // Lifting
        case LiftBlobIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftCallableStatementIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftClobIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftConnectionIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftDriverIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftNClobIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftPreparedStatementIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftRefIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftResultSetIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftSQLDataIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftSQLInputIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftSQLOutputIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
        case LiftStatementIO(s, k) => Kleisli(_ => k.liftK[M].run(s))
  
        // Combinators
        case Pure(a) => primitive(_ => a())
        case Attempt(a) => a.liftK[M].attempt
  
        // Primitive Operations
        case AllProceduresAreCallable => primitive(_.allProceduresAreCallable)
        case AllTablesAreSelectable => primitive(_.allTablesAreSelectable)
        case AutoCommitFailureClosesAllResultSets => primitive(_.autoCommitFailureClosesAllResultSets)
        case DataDefinitionCausesTransactionCommit => primitive(_.dataDefinitionCausesTransactionCommit)
        case DataDefinitionIgnoredInTransactions => primitive(_.dataDefinitionIgnoredInTransactions)
        case DeletesAreDetected(a) => primitive(_.deletesAreDetected(a))
        case DoesMaxRowSizeIncludeBlobs => primitive(_.doesMaxRowSizeIncludeBlobs)
        case GetAttributes(a, b, c, d) => primitive(_.getAttributes(a, b, c, d))
        case GetBestRowIdentifier(a, b, c, d, e) => primitive(_.getBestRowIdentifier(a, b, c, d, e))
        case GetCatalogSeparator => primitive(_.getCatalogSeparator)
        case GetCatalogTerm => primitive(_.getCatalogTerm)
        case GetCatalogs => primitive(_.getCatalogs)
        case GetClientInfoProperties => primitive(_.getClientInfoProperties)
        case GetColumnPrivileges(a, b, c, d) => primitive(_.getColumnPrivileges(a, b, c, d))
        case GetColumns(a, b, c, d) => primitive(_.getColumns(a, b, c, d))
        case GetConnection => primitive(_.getConnection)
        case GetCrossReference(a, b, c, d, e, f) => primitive(_.getCrossReference(a, b, c, d, e, f))
        case GetDatabaseMajorVersion => primitive(_.getDatabaseMajorVersion)
        case GetDatabaseMinorVersion => primitive(_.getDatabaseMinorVersion)
        case GetDatabaseProductName => primitive(_.getDatabaseProductName)
        case GetDatabaseProductVersion => primitive(_.getDatabaseProductVersion)
        case GetDefaultTransactionIsolation => primitive(_.getDefaultTransactionIsolation)
        case GetDriverMajorVersion => primitive(_.getDriverMajorVersion)
        case GetDriverMinorVersion => primitive(_.getDriverMinorVersion)
        case GetDriverName => primitive(_.getDriverName)
        case GetDriverVersion => primitive(_.getDriverVersion)
        case GetExportedKeys(a, b, c) => primitive(_.getExportedKeys(a, b, c))
        case GetExtraNameCharacters => primitive(_.getExtraNameCharacters)
        case GetFunctionColumns(a, b, c, d) => primitive(_.getFunctionColumns(a, b, c, d))
        case GetFunctions(a, b, c) => primitive(_.getFunctions(a, b, c))
        case GetIdentifierQuoteString => primitive(_.getIdentifierQuoteString)
        case GetImportedKeys(a, b, c) => primitive(_.getImportedKeys(a, b, c))
        case GetIndexInfo(a, b, c, d, e) => primitive(_.getIndexInfo(a, b, c, d, e))
        case GetJDBCMajorVersion => primitive(_.getJDBCMajorVersion)
        case GetJDBCMinorVersion => primitive(_.getJDBCMinorVersion)
        case GetMaxBinaryLiteralLength => primitive(_.getMaxBinaryLiteralLength)
        case GetMaxCatalogNameLength => primitive(_.getMaxCatalogNameLength)
        case GetMaxCharLiteralLength => primitive(_.getMaxCharLiteralLength)
        case GetMaxColumnNameLength => primitive(_.getMaxColumnNameLength)
        case GetMaxColumnsInGroupBy => primitive(_.getMaxColumnsInGroupBy)
        case GetMaxColumnsInIndex => primitive(_.getMaxColumnsInIndex)
        case GetMaxColumnsInOrderBy => primitive(_.getMaxColumnsInOrderBy)
        case GetMaxColumnsInSelect => primitive(_.getMaxColumnsInSelect)
        case GetMaxColumnsInTable => primitive(_.getMaxColumnsInTable)
        case GetMaxConnections => primitive(_.getMaxConnections)
        case GetMaxCursorNameLength => primitive(_.getMaxCursorNameLength)
        case GetMaxIndexLength => primitive(_.getMaxIndexLength)
        case GetMaxProcedureNameLength => primitive(_.getMaxProcedureNameLength)
        case GetMaxRowSize => primitive(_.getMaxRowSize)
        case GetMaxSchemaNameLength => primitive(_.getMaxSchemaNameLength)
        case GetMaxStatementLength => primitive(_.getMaxStatementLength)
        case GetMaxStatements => primitive(_.getMaxStatements)
        case GetMaxTableNameLength => primitive(_.getMaxTableNameLength)
        case GetMaxTablesInSelect => primitive(_.getMaxTablesInSelect)
        case GetMaxUserNameLength => primitive(_.getMaxUserNameLength)
        case GetNumericFunctions => primitive(_.getNumericFunctions)
        case GetPrimaryKeys(a, b, c) => primitive(_.getPrimaryKeys(a, b, c))
        case GetProcedureColumns(a, b, c, d) => primitive(_.getProcedureColumns(a, b, c, d))
        case GetProcedureTerm => primitive(_.getProcedureTerm)
        case GetProcedures(a, b, c) => primitive(_.getProcedures(a, b, c))
        case GetResultSetHoldability => primitive(_.getResultSetHoldability)
        case GetRowIdLifetime => primitive(_.getRowIdLifetime)
        case GetSQLKeywords => primitive(_.getSQLKeywords)
        case GetSQLStateType => primitive(_.getSQLStateType)
        case GetSchemaTerm => primitive(_.getSchemaTerm)
        case GetSchemas => primitive(_.getSchemas)
        case GetSchemas1(a, b) => primitive(_.getSchemas(a, b))
        case GetSearchStringEscape => primitive(_.getSearchStringEscape)
        case GetStringFunctions => primitive(_.getStringFunctions)
        case GetSuperTables(a, b, c) => primitive(_.getSuperTables(a, b, c))
        case GetSuperTypes(a, b, c) => primitive(_.getSuperTypes(a, b, c))
        case GetSystemFunctions => primitive(_.getSystemFunctions)
        case GetTablePrivileges(a, b, c) => primitive(_.getTablePrivileges(a, b, c))
        case GetTableTypes => primitive(_.getTableTypes)
        case GetTables(a, b, c, d) => primitive(_.getTables(a, b, c, d))
        case GetTimeDateFunctions => primitive(_.getTimeDateFunctions)
        case GetTypeInfo => primitive(_.getTypeInfo)
        case GetUDTs(a, b, c, d) => primitive(_.getUDTs(a, b, c, d))
        case GetURL => primitive(_.getURL)
        case GetUserName => primitive(_.getUserName)
        case GetVersionColumns(a, b, c) => primitive(_.getVersionColumns(a, b, c))
        case InsertsAreDetected(a) => primitive(_.insertsAreDetected(a))
        case IsCatalogAtStart => primitive(_.isCatalogAtStart)
        case IsReadOnly => primitive(_.isReadOnly)
        case IsWrapperFor(a) => primitive(_.isWrapperFor(a))
        case LocatorsUpdateCopy => primitive(_.locatorsUpdateCopy)
        case NullPlusNonNullIsNull => primitive(_.nullPlusNonNullIsNull)
        case NullsAreSortedAtEnd => primitive(_.nullsAreSortedAtEnd)
        case NullsAreSortedAtStart => primitive(_.nullsAreSortedAtStart)
        case NullsAreSortedHigh => primitive(_.nullsAreSortedHigh)
        case NullsAreSortedLow => primitive(_.nullsAreSortedLow)
        case OthersDeletesAreVisible(a) => primitive(_.othersDeletesAreVisible(a))
        case OthersInsertsAreVisible(a) => primitive(_.othersInsertsAreVisible(a))
        case OthersUpdatesAreVisible(a) => primitive(_.othersUpdatesAreVisible(a))
        case OwnDeletesAreVisible(a) => primitive(_.ownDeletesAreVisible(a))
        case OwnInsertsAreVisible(a) => primitive(_.ownInsertsAreVisible(a))
        case OwnUpdatesAreVisible(a) => primitive(_.ownUpdatesAreVisible(a))
        case StoresLowerCaseIdentifiers => primitive(_.storesLowerCaseIdentifiers)
        case StoresLowerCaseQuotedIdentifiers => primitive(_.storesLowerCaseQuotedIdentifiers)
        case StoresMixedCaseIdentifiers => primitive(_.storesMixedCaseIdentifiers)
        case StoresMixedCaseQuotedIdentifiers => primitive(_.storesMixedCaseQuotedIdentifiers)
        case StoresUpperCaseIdentifiers => primitive(_.storesUpperCaseIdentifiers)
        case StoresUpperCaseQuotedIdentifiers => primitive(_.storesUpperCaseQuotedIdentifiers)
        case SupportsANSI92EntryLevelSQL => primitive(_.supportsANSI92EntryLevelSQL)
        case SupportsANSI92FullSQL => primitive(_.supportsANSI92FullSQL)
        case SupportsANSI92IntermediateSQL => primitive(_.supportsANSI92IntermediateSQL)
        case SupportsAlterTableWithAddColumn => primitive(_.supportsAlterTableWithAddColumn)
        case SupportsAlterTableWithDropColumn => primitive(_.supportsAlterTableWithDropColumn)
        case SupportsBatchUpdates => primitive(_.supportsBatchUpdates)
        case SupportsCatalogsInDataManipulation => primitive(_.supportsCatalogsInDataManipulation)
        case SupportsCatalogsInIndexDefinitions => primitive(_.supportsCatalogsInIndexDefinitions)
        case SupportsCatalogsInPrivilegeDefinitions => primitive(_.supportsCatalogsInPrivilegeDefinitions)
        case SupportsCatalogsInProcedureCalls => primitive(_.supportsCatalogsInProcedureCalls)
        case SupportsCatalogsInTableDefinitions => primitive(_.supportsCatalogsInTableDefinitions)
        case SupportsColumnAliasing => primitive(_.supportsColumnAliasing)
        case SupportsConvert(a, b) => primitive(_.supportsConvert(a, b))
        case SupportsConvert1 => primitive(_.supportsConvert)
        case SupportsCoreSQLGrammar => primitive(_.supportsCoreSQLGrammar)
        case SupportsCorrelatedSubqueries => primitive(_.supportsCorrelatedSubqueries)
        case SupportsDataDefinitionAndDataManipulationTransactions => primitive(_.supportsDataDefinitionAndDataManipulationTransactions)
        case SupportsDataManipulationTransactionsOnly => primitive(_.supportsDataManipulationTransactionsOnly)
        case SupportsDifferentTableCorrelationNames => primitive(_.supportsDifferentTableCorrelationNames)
        case SupportsExpressionsInOrderBy => primitive(_.supportsExpressionsInOrderBy)
        case SupportsExtendedSQLGrammar => primitive(_.supportsExtendedSQLGrammar)
        case SupportsFullOuterJoins => primitive(_.supportsFullOuterJoins)
        case SupportsGetGeneratedKeys => primitive(_.supportsGetGeneratedKeys)
        case SupportsGroupBy => primitive(_.supportsGroupBy)
        case SupportsGroupByBeyondSelect => primitive(_.supportsGroupByBeyondSelect)
        case SupportsGroupByUnrelated => primitive(_.supportsGroupByUnrelated)
        case SupportsIntegrityEnhancementFacility => primitive(_.supportsIntegrityEnhancementFacility)
        case SupportsLikeEscapeClause => primitive(_.supportsLikeEscapeClause)
        case SupportsLimitedOuterJoins => primitive(_.supportsLimitedOuterJoins)
        case SupportsMinimumSQLGrammar => primitive(_.supportsMinimumSQLGrammar)
        case SupportsMixedCaseIdentifiers => primitive(_.supportsMixedCaseIdentifiers)
        case SupportsMixedCaseQuotedIdentifiers => primitive(_.supportsMixedCaseQuotedIdentifiers)
        case SupportsMultipleOpenResults => primitive(_.supportsMultipleOpenResults)
        case SupportsMultipleResultSets => primitive(_.supportsMultipleResultSets)
        case SupportsMultipleTransactions => primitive(_.supportsMultipleTransactions)
        case SupportsNamedParameters => primitive(_.supportsNamedParameters)
        case SupportsNonNullableColumns => primitive(_.supportsNonNullableColumns)
        case SupportsOpenCursorsAcrossCommit => primitive(_.supportsOpenCursorsAcrossCommit)
        case SupportsOpenCursorsAcrossRollback => primitive(_.supportsOpenCursorsAcrossRollback)
        case SupportsOpenStatementsAcrossCommit => primitive(_.supportsOpenStatementsAcrossCommit)
        case SupportsOpenStatementsAcrossRollback => primitive(_.supportsOpenStatementsAcrossRollback)
        case SupportsOrderByUnrelated => primitive(_.supportsOrderByUnrelated)
        case SupportsOuterJoins => primitive(_.supportsOuterJoins)
        case SupportsPositionedDelete => primitive(_.supportsPositionedDelete)
        case SupportsPositionedUpdate => primitive(_.supportsPositionedUpdate)
        case SupportsResultSetConcurrency(a, b) => primitive(_.supportsResultSetConcurrency(a, b))
        case SupportsResultSetHoldability(a) => primitive(_.supportsResultSetHoldability(a))
        case SupportsResultSetType(a) => primitive(_.supportsResultSetType(a))
        case SupportsSavepoints => primitive(_.supportsSavepoints)
        case SupportsSchemasInDataManipulation => primitive(_.supportsSchemasInDataManipulation)
        case SupportsSchemasInIndexDefinitions => primitive(_.supportsSchemasInIndexDefinitions)
        case SupportsSchemasInPrivilegeDefinitions => primitive(_.supportsSchemasInPrivilegeDefinitions)
        case SupportsSchemasInProcedureCalls => primitive(_.supportsSchemasInProcedureCalls)
        case SupportsSchemasInTableDefinitions => primitive(_.supportsSchemasInTableDefinitions)
        case SupportsSelectForUpdate => primitive(_.supportsSelectForUpdate)
        case SupportsStatementPooling => primitive(_.supportsStatementPooling)
        case SupportsStoredFunctionsUsingCallSyntax => primitive(_.supportsStoredFunctionsUsingCallSyntax)
        case SupportsStoredProcedures => primitive(_.supportsStoredProcedures)
        case SupportsSubqueriesInComparisons => primitive(_.supportsSubqueriesInComparisons)
        case SupportsSubqueriesInExists => primitive(_.supportsSubqueriesInExists)
        case SupportsSubqueriesInIns => primitive(_.supportsSubqueriesInIns)
        case SupportsSubqueriesInQuantifieds => primitive(_.supportsSubqueriesInQuantifieds)
        case SupportsTableCorrelationNames => primitive(_.supportsTableCorrelationNames)
        case SupportsTransactionIsolationLevel(a) => primitive(_.supportsTransactionIsolationLevel(a))
        case SupportsTransactions => primitive(_.supportsTransactions)
        case SupportsUnion => primitive(_.supportsUnion)
        case SupportsUnionAll => primitive(_.supportsUnionAll)
        case Unwrap(a) => primitive(_.unwrap(a))
        case UpdatesAreDetected(a) => primitive(_.updatesAreDetected(a))
        case UsesLocalFilePerTable => primitive(_.usesLocalFilePerTable)
        case UsesLocalFiles => primitive(_.usesLocalFiles)
  
      }
  
    }

  /**
   * Syntax for `DatabaseMetaDataIO`.
   * @group Algebra
   */
  implicit class DatabaseMetaDataIOOps[A](ma: DatabaseMetaDataIO[A]) {
    def liftK[M[_]: Monad: Catchable: Capture]: Kleisli[M, DatabaseMetaData, A] =
      F.runFC[DatabaseMetaDataOp,({type l[a]=Kleisli[M,DatabaseMetaData,a]})#l,A](ma)(kleisliTrans[M])
  }

}

