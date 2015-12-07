package doobie.free

import scalaz.{ Catchable, Coyoneda, Free => F, Kleisli, Monad, ~>, \/ }
import scalaz.concurrent.Task

import doobie.util.capture._
import doobie.free.kleislitrans._

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
 * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
 *
 * {{{
 * // An action to run
 * val a: DatabaseMetaDataIO[Foo] = ...
 * 
 * // A JDBC object 
 * val s: DatabaseMetaData = ...
 * 
 * // Unfolding into a Task
 * val ta: Task[A] = a.transK[Task].run(s)
 * }}}
 *
 * @group Modules
 */
object databasemetadata {
  
  /** 
   * Sum type of primitive operations over a `java.sql.DatabaseMetaData`.
   * @group Algebra 
   */
  sealed trait DatabaseMetaDataOp[A] {
    protected def primitive[M[_]: Monad: Capture](f: DatabaseMetaData => A): Kleisli[M, DatabaseMetaData, A] = 
      Kleisli((s: DatabaseMetaData) => Capture[M].apply(f(s)))
    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, DatabaseMetaData, A]
  }

  /** 
   * Module of constructors for `DatabaseMetaDataOp`. These are rarely useful outside of the implementation;
   * prefer the smart constructors provided by the `databasemetadata` module.
   * @group Algebra 
   */
  object DatabaseMetaDataOp {
    
    // This algebra has a default interpreter
    implicit val DatabaseMetaDataKleisliTrans: KleisliTrans.Aux[DatabaseMetaDataOp, DatabaseMetaData] =
      new KleisliTrans[DatabaseMetaDataOp] {
        type J = DatabaseMetaData
        def interpK[M[_]: Monad: Catchable: Capture]: DatabaseMetaDataOp ~> Kleisli[M, DatabaseMetaData, ?] =
          new (DatabaseMetaDataOp ~> Kleisli[M, DatabaseMetaData, ?]) {
            def apply[A](op: DatabaseMetaDataOp[A]): Kleisli[M, DatabaseMetaData, A] =
              op.defaultTransK[M]
          }
      }

    // Lifting
    case class Lift[Op[_], A, J](j: J, action: F.FreeC[Op, A], mod: KleisliTrans.Aux[Op, J]) extends DatabaseMetaDataOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => mod.transK[M].apply(action).run(j))
    }

    // Combinators
    case class Attempt[A](action: DatabaseMetaDataIO[A]) extends DatabaseMetaDataOp[Throwable \/ A] {
      import scalaz._, Scalaz._
      def defaultTransK[M[_]: Monad: Catchable: Capture] = 
        Predef.implicitly[Catchable[Kleisli[M, DatabaseMetaData, ?]]].attempt(action.transK[M])
    }
    case class Pure[A](a: () => A) extends DatabaseMetaDataOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
    }
    case class Raw[A](f: DatabaseMetaData => A) extends DatabaseMetaDataOp[A] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
    }

    // Primitive Operations
    case object AllProceduresAreCallable extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.allProceduresAreCallable())
    }
    case object AllTablesAreSelectable extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.allTablesAreSelectable())
    }
    case object AutoCommitFailureClosesAllResultSets extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.autoCommitFailureClosesAllResultSets())
    }
    case object DataDefinitionCausesTransactionCommit extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.dataDefinitionCausesTransactionCommit())
    }
    case object DataDefinitionIgnoredInTransactions extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.dataDefinitionIgnoredInTransactions())
    }
    case class  DeletesAreDetected(a: Int) extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.deletesAreDetected(a))
    }
    case object DoesMaxRowSizeIncludeBlobs extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.doesMaxRowSizeIncludeBlobs())
    }
    case object GeneratedKeyAlwaysReturned extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.generatedKeyAlwaysReturned())
    }
    case class  GetAttributes(a: String, b: String, c: String, d: String) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getAttributes(a, b, c, d))
    }
    case class  GetBestRowIdentifier(a: String, b: String, c: String, d: Int, e: Boolean) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getBestRowIdentifier(a, b, c, d, e))
    }
    case object GetCatalogSeparator extends DatabaseMetaDataOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCatalogSeparator())
    }
    case object GetCatalogTerm extends DatabaseMetaDataOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCatalogTerm())
    }
    case object GetCatalogs extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCatalogs())
    }
    case object GetClientInfoProperties extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getClientInfoProperties())
    }
    case class  GetColumnPrivileges(a: String, b: String, c: String, d: String) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getColumnPrivileges(a, b, c, d))
    }
    case class  GetColumns(a: String, b: String, c: String, d: String) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getColumns(a, b, c, d))
    }
    case object GetConnection extends DatabaseMetaDataOp[Connection] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getConnection())
    }
    case class  GetCrossReference(a: String, b: String, c: String, d: String, e: String, f: String) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getCrossReference(a, b, c, d, e, f))
    }
    case object GetDatabaseMajorVersion extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDatabaseMajorVersion())
    }
    case object GetDatabaseMinorVersion extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDatabaseMinorVersion())
    }
    case object GetDatabaseProductName extends DatabaseMetaDataOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDatabaseProductName())
    }
    case object GetDatabaseProductVersion extends DatabaseMetaDataOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDatabaseProductVersion())
    }
    case object GetDefaultTransactionIsolation extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDefaultTransactionIsolation())
    }
    case object GetDriverMajorVersion extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDriverMajorVersion())
    }
    case object GetDriverMinorVersion extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDriverMinorVersion())
    }
    case object GetDriverName extends DatabaseMetaDataOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDriverName())
    }
    case object GetDriverVersion extends DatabaseMetaDataOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getDriverVersion())
    }
    case class  GetExportedKeys(a: String, b: String, c: String) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getExportedKeys(a, b, c))
    }
    case object GetExtraNameCharacters extends DatabaseMetaDataOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getExtraNameCharacters())
    }
    case class  GetFunctionColumns(a: String, b: String, c: String, d: String) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getFunctionColumns(a, b, c, d))
    }
    case class  GetFunctions(a: String, b: String, c: String) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getFunctions(a, b, c))
    }
    case object GetIdentifierQuoteString extends DatabaseMetaDataOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getIdentifierQuoteString())
    }
    case class  GetImportedKeys(a: String, b: String, c: String) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getImportedKeys(a, b, c))
    }
    case class  GetIndexInfo(a: String, b: String, c: String, d: Boolean, e: Boolean) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getIndexInfo(a, b, c, d, e))
    }
    case object GetJDBCMajorVersion extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getJDBCMajorVersion())
    }
    case object GetJDBCMinorVersion extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getJDBCMinorVersion())
    }
    case object GetMaxBinaryLiteralLength extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxBinaryLiteralLength())
    }
    case object GetMaxCatalogNameLength extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxCatalogNameLength())
    }
    case object GetMaxCharLiteralLength extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxCharLiteralLength())
    }
    case object GetMaxColumnNameLength extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxColumnNameLength())
    }
    case object GetMaxColumnsInGroupBy extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxColumnsInGroupBy())
    }
    case object GetMaxColumnsInIndex extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxColumnsInIndex())
    }
    case object GetMaxColumnsInOrderBy extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxColumnsInOrderBy())
    }
    case object GetMaxColumnsInSelect extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxColumnsInSelect())
    }
    case object GetMaxColumnsInTable extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxColumnsInTable())
    }
    case object GetMaxConnections extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxConnections())
    }
    case object GetMaxCursorNameLength extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxCursorNameLength())
    }
    case object GetMaxIndexLength extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxIndexLength())
    }
    case object GetMaxLogicalLobSize extends DatabaseMetaDataOp[Long] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxLogicalLobSize())
    }
    case object GetMaxProcedureNameLength extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxProcedureNameLength())
    }
    case object GetMaxRowSize extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxRowSize())
    }
    case object GetMaxSchemaNameLength extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxSchemaNameLength())
    }
    case object GetMaxStatementLength extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxStatementLength())
    }
    case object GetMaxStatements extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxStatements())
    }
    case object GetMaxTableNameLength extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxTableNameLength())
    }
    case object GetMaxTablesInSelect extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxTablesInSelect())
    }
    case object GetMaxUserNameLength extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getMaxUserNameLength())
    }
    case object GetNumericFunctions extends DatabaseMetaDataOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getNumericFunctions())
    }
    case class  GetPrimaryKeys(a: String, b: String, c: String) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getPrimaryKeys(a, b, c))
    }
    case class  GetProcedureColumns(a: String, b: String, c: String, d: String) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getProcedureColumns(a, b, c, d))
    }
    case object GetProcedureTerm extends DatabaseMetaDataOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getProcedureTerm())
    }
    case class  GetProcedures(a: String, b: String, c: String) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getProcedures(a, b, c))
    }
    case class  GetPseudoColumns(a: String, b: String, c: String, d: String) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getPseudoColumns(a, b, c, d))
    }
    case object GetResultSetHoldability extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getResultSetHoldability())
    }
    case object GetRowIdLifetime extends DatabaseMetaDataOp[RowIdLifetime] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getRowIdLifetime())
    }
    case object GetSQLKeywords extends DatabaseMetaDataOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSQLKeywords())
    }
    case object GetSQLStateType extends DatabaseMetaDataOp[Int] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSQLStateType())
    }
    case object GetSchemaTerm extends DatabaseMetaDataOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSchemaTerm())
    }
    case class  GetSchemas(a: String, b: String) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSchemas(a, b))
    }
    case object GetSchemas1 extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSchemas())
    }
    case object GetSearchStringEscape extends DatabaseMetaDataOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSearchStringEscape())
    }
    case object GetStringFunctions extends DatabaseMetaDataOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getStringFunctions())
    }
    case class  GetSuperTables(a: String, b: String, c: String) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSuperTables(a, b, c))
    }
    case class  GetSuperTypes(a: String, b: String, c: String) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSuperTypes(a, b, c))
    }
    case object GetSystemFunctions extends DatabaseMetaDataOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getSystemFunctions())
    }
    case class  GetTablePrivileges(a: String, b: String, c: String) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTablePrivileges(a, b, c))
    }
    case object GetTableTypes extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTableTypes())
    }
    case class  GetTables(a: String, b: String, c: String, d: Array[String]) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTables(a, b, c, d))
    }
    case object GetTimeDateFunctions extends DatabaseMetaDataOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTimeDateFunctions())
    }
    case object GetTypeInfo extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getTypeInfo())
    }
    case class  GetUDTs(a: String, b: String, c: String, d: Array[Int]) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getUDTs(a, b, c, d))
    }
    case object GetURL extends DatabaseMetaDataOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getURL())
    }
    case object GetUserName extends DatabaseMetaDataOp[String] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getUserName())
    }
    case class  GetVersionColumns(a: String, b: String, c: String) extends DatabaseMetaDataOp[ResultSet] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.getVersionColumns(a, b, c))
    }
    case class  InsertsAreDetected(a: Int) extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.insertsAreDetected(a))
    }
    case object IsCatalogAtStart extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isCatalogAtStart())
    }
    case object IsReadOnly extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isReadOnly())
    }
    case class  IsWrapperFor(a: Class[_]) extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.isWrapperFor(a))
    }
    case object LocatorsUpdateCopy extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.locatorsUpdateCopy())
    }
    case object NullPlusNonNullIsNull extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.nullPlusNonNullIsNull())
    }
    case object NullsAreSortedAtEnd extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.nullsAreSortedAtEnd())
    }
    case object NullsAreSortedAtStart extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.nullsAreSortedAtStart())
    }
    case object NullsAreSortedHigh extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.nullsAreSortedHigh())
    }
    case object NullsAreSortedLow extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.nullsAreSortedLow())
    }
    case class  OthersDeletesAreVisible(a: Int) extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.othersDeletesAreVisible(a))
    }
    case class  OthersInsertsAreVisible(a: Int) extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.othersInsertsAreVisible(a))
    }
    case class  OthersUpdatesAreVisible(a: Int) extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.othersUpdatesAreVisible(a))
    }
    case class  OwnDeletesAreVisible(a: Int) extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.ownDeletesAreVisible(a))
    }
    case class  OwnInsertsAreVisible(a: Int) extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.ownInsertsAreVisible(a))
    }
    case class  OwnUpdatesAreVisible(a: Int) extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.ownUpdatesAreVisible(a))
    }
    case object StoresLowerCaseIdentifiers extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.storesLowerCaseIdentifiers())
    }
    case object StoresLowerCaseQuotedIdentifiers extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.storesLowerCaseQuotedIdentifiers())
    }
    case object StoresMixedCaseIdentifiers extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.storesMixedCaseIdentifiers())
    }
    case object StoresMixedCaseQuotedIdentifiers extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.storesMixedCaseQuotedIdentifiers())
    }
    case object StoresUpperCaseIdentifiers extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.storesUpperCaseIdentifiers())
    }
    case object StoresUpperCaseQuotedIdentifiers extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.storesUpperCaseQuotedIdentifiers())
    }
    case object SupportsANSI92EntryLevelSQL extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsANSI92EntryLevelSQL())
    }
    case object SupportsANSI92FullSQL extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsANSI92FullSQL())
    }
    case object SupportsANSI92IntermediateSQL extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsANSI92IntermediateSQL())
    }
    case object SupportsAlterTableWithAddColumn extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsAlterTableWithAddColumn())
    }
    case object SupportsAlterTableWithDropColumn extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsAlterTableWithDropColumn())
    }
    case object SupportsBatchUpdates extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsBatchUpdates())
    }
    case object SupportsCatalogsInDataManipulation extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsCatalogsInDataManipulation())
    }
    case object SupportsCatalogsInIndexDefinitions extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsCatalogsInIndexDefinitions())
    }
    case object SupportsCatalogsInPrivilegeDefinitions extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsCatalogsInPrivilegeDefinitions())
    }
    case object SupportsCatalogsInProcedureCalls extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsCatalogsInProcedureCalls())
    }
    case object SupportsCatalogsInTableDefinitions extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsCatalogsInTableDefinitions())
    }
    case object SupportsColumnAliasing extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsColumnAliasing())
    }
    case class  SupportsConvert(a: Int, b: Int) extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsConvert(a, b))
    }
    case object SupportsConvert1 extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsConvert())
    }
    case object SupportsCoreSQLGrammar extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsCoreSQLGrammar())
    }
    case object SupportsCorrelatedSubqueries extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsCorrelatedSubqueries())
    }
    case object SupportsDataDefinitionAndDataManipulationTransactions extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsDataDefinitionAndDataManipulationTransactions())
    }
    case object SupportsDataManipulationTransactionsOnly extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsDataManipulationTransactionsOnly())
    }
    case object SupportsDifferentTableCorrelationNames extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsDifferentTableCorrelationNames())
    }
    case object SupportsExpressionsInOrderBy extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsExpressionsInOrderBy())
    }
    case object SupportsExtendedSQLGrammar extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsExtendedSQLGrammar())
    }
    case object SupportsFullOuterJoins extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsFullOuterJoins())
    }
    case object SupportsGetGeneratedKeys extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsGetGeneratedKeys())
    }
    case object SupportsGroupBy extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsGroupBy())
    }
    case object SupportsGroupByBeyondSelect extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsGroupByBeyondSelect())
    }
    case object SupportsGroupByUnrelated extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsGroupByUnrelated())
    }
    case object SupportsIntegrityEnhancementFacility extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsIntegrityEnhancementFacility())
    }
    case object SupportsLikeEscapeClause extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsLikeEscapeClause())
    }
    case object SupportsLimitedOuterJoins extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsLimitedOuterJoins())
    }
    case object SupportsMinimumSQLGrammar extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsMinimumSQLGrammar())
    }
    case object SupportsMixedCaseIdentifiers extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsMixedCaseIdentifiers())
    }
    case object SupportsMixedCaseQuotedIdentifiers extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsMixedCaseQuotedIdentifiers())
    }
    case object SupportsMultipleOpenResults extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsMultipleOpenResults())
    }
    case object SupportsMultipleResultSets extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsMultipleResultSets())
    }
    case object SupportsMultipleTransactions extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsMultipleTransactions())
    }
    case object SupportsNamedParameters extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsNamedParameters())
    }
    case object SupportsNonNullableColumns extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsNonNullableColumns())
    }
    case object SupportsOpenCursorsAcrossCommit extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsOpenCursorsAcrossCommit())
    }
    case object SupportsOpenCursorsAcrossRollback extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsOpenCursorsAcrossRollback())
    }
    case object SupportsOpenStatementsAcrossCommit extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsOpenStatementsAcrossCommit())
    }
    case object SupportsOpenStatementsAcrossRollback extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsOpenStatementsAcrossRollback())
    }
    case object SupportsOrderByUnrelated extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsOrderByUnrelated())
    }
    case object SupportsOuterJoins extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsOuterJoins())
    }
    case object SupportsPositionedDelete extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsPositionedDelete())
    }
    case object SupportsPositionedUpdate extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsPositionedUpdate())
    }
    case object SupportsRefCursors extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsRefCursors())
    }
    case class  SupportsResultSetConcurrency(a: Int, b: Int) extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsResultSetConcurrency(a, b))
    }
    case class  SupportsResultSetHoldability(a: Int) extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsResultSetHoldability(a))
    }
    case class  SupportsResultSetType(a: Int) extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsResultSetType(a))
    }
    case object SupportsSavepoints extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsSavepoints())
    }
    case object SupportsSchemasInDataManipulation extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsSchemasInDataManipulation())
    }
    case object SupportsSchemasInIndexDefinitions extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsSchemasInIndexDefinitions())
    }
    case object SupportsSchemasInPrivilegeDefinitions extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsSchemasInPrivilegeDefinitions())
    }
    case object SupportsSchemasInProcedureCalls extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsSchemasInProcedureCalls())
    }
    case object SupportsSchemasInTableDefinitions extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsSchemasInTableDefinitions())
    }
    case object SupportsSelectForUpdate extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsSelectForUpdate())
    }
    case object SupportsStatementPooling extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsStatementPooling())
    }
    case object SupportsStoredFunctionsUsingCallSyntax extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsStoredFunctionsUsingCallSyntax())
    }
    case object SupportsStoredProcedures extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsStoredProcedures())
    }
    case object SupportsSubqueriesInComparisons extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsSubqueriesInComparisons())
    }
    case object SupportsSubqueriesInExists extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsSubqueriesInExists())
    }
    case object SupportsSubqueriesInIns extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsSubqueriesInIns())
    }
    case object SupportsSubqueriesInQuantifieds extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsSubqueriesInQuantifieds())
    }
    case object SupportsTableCorrelationNames extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsTableCorrelationNames())
    }
    case class  SupportsTransactionIsolationLevel(a: Int) extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsTransactionIsolationLevel(a))
    }
    case object SupportsTransactions extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsTransactions())
    }
    case object SupportsUnion extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsUnion())
    }
    case object SupportsUnionAll extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.supportsUnionAll())
    }
    case class  Unwrap[T](a: Class[T]) extends DatabaseMetaDataOp[T] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.unwrap(a))
    }
    case class  UpdatesAreDetected(a: Int) extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.updatesAreDetected(a))
    }
    case object UsesLocalFilePerTable extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.usesLocalFilePerTable())
    }
    case object UsesLocalFiles extends DatabaseMetaDataOp[Boolean] {
      def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_.usesLocalFiles())
    }

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
   * Capture instance for [[DatabaseMetaDataIO]].
   * @group Typeclass Instances
   */
  implicit val CaptureDatabaseMetaDataIO: Capture[DatabaseMetaDataIO] =
    new Capture[DatabaseMetaDataIO] {
      def apply[A](a: => A): DatabaseMetaDataIO[A] = databasemetadata.delay(a)
    }

  /**
   * Lift a different type of program that has a default Kleisli interpreter.
   * @group Constructors (Lifting)
   */
  def lift[Op[_], A, J](j: J, action: F.FreeC[Op, A])(implicit mod: KleisliTrans.Aux[Op, J]): DatabaseMetaDataIO[A] =
    F.liftFC(Lift(j, action, mod))

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
   * Backdoor for arbitrary computations on the underlying DatabaseMetaData.
   * @group Constructors (Lifting)
   */
  def raw[A](f: DatabaseMetaData => A): DatabaseMetaDataIO[A] =
    F.liftFC(Raw(f))

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
  val generatedKeyAlwaysReturned: DatabaseMetaDataIO[Boolean] =
    F.liftFC(GeneratedKeyAlwaysReturned)

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
  val getMaxLogicalLobSize: DatabaseMetaDataIO[Long] =
    F.liftFC(GetMaxLogicalLobSize)

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
  def getPseudoColumns(a: String, b: String, c: String, d: String): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetPseudoColumns(a, b, c, d))

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
  def getSchemas(a: String, b: String): DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetSchemas(a, b))

  /** 
   * @group Constructors (Primitives)
   */
  val getSchemas: DatabaseMetaDataIO[ResultSet] =
    F.liftFC(GetSchemas1)

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
  val supportsRefCursors: DatabaseMetaDataIO[Boolean] =
    F.liftFC(SupportsRefCursors)

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
  def interpK[M[_]: Monad: Catchable: Capture]: DatabaseMetaDataOp ~> Kleisli[M, DatabaseMetaData, ?] =
   DatabaseMetaDataOp.DatabaseMetaDataKleisliTrans.interpK

 /** 
  * Natural transformation from `DatabaseMetaDataIO` to `Kleisli` for the given `M`, consuming a `java.sql.DatabaseMetaData`. 
  * @group Algebra
  */
  def transK[M[_]: Monad: Catchable: Capture]: DatabaseMetaDataIO ~> Kleisli[M, DatabaseMetaData, ?] =
   DatabaseMetaDataOp.DatabaseMetaDataKleisliTrans.transK

 /** 
  * Natural transformation from `DatabaseMetaDataIO` to `M`, given a `java.sql.DatabaseMetaData`. 
  * @group Algebra
  */
 def trans[M[_]: Monad: Catchable: Capture](c: DatabaseMetaData): DatabaseMetaDataIO ~> M =
   DatabaseMetaDataOp.DatabaseMetaDataKleisliTrans.trans[M](c)

  /**
   * Syntax for `DatabaseMetaDataIO`.
   * @group Algebra
   */
  implicit class DatabaseMetaDataIOOps[A](ma: DatabaseMetaDataIO[A]) {
    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, DatabaseMetaData, A] =
      DatabaseMetaDataOp.DatabaseMetaDataKleisliTrans.transK[M].apply(ma)
  }

}

