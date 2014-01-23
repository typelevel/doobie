package doobie.dbc
package op

import enum._
import scalaz.effect.IO
import java.sql
import java.sql._

// TODO: review enum usage
trait DatabaseMetaDataOps extends PrimitiveOps[sql.DatabaseMetaData] {

  def allProceduresAreCallable: Action[Boolean] =
    primitive(s"allProceduresAreCallable", _.allProceduresAreCallable)

  def allTablesAreSelectable: Action[Boolean] =
    primitive(s"allTablesAreSelectable", _.allTablesAreSelectable)

  def autoCommitFailureClosesAllResultSets: Action[Boolean] =
    primitive(s"autoCommitFailureClosesAllResultSets", _.autoCommitFailureClosesAllResultSets)

  def dataDefinitionCausesTransactionCommit: Action[Boolean] =
    primitive(s"dataDefinitionCausesTransactionCommit", _.dataDefinitionCausesTransactionCommit)

  def dataDefinitionIgnoredInTransactions: Action[Boolean] =
    primitive(s"dataDefinitionIgnoredInTransactions", _.dataDefinitionIgnoredInTransactions)

  def deletesAreDetected(kind: Int): Action[Boolean] =
    primitive(s"deletesAreDetected($kind)", _.deletesAreDetected(kind))

  def doesMaxRowSizeIncludeBlobs: Action[Boolean] =
    primitive(s"doesMaxRowSizeIncludeBlobs", _.doesMaxRowSizeIncludeBlobs)

  def getAttributes[A](catalog: String, schemaPattern: String, typeNamePattern: String, attributeNamePattern: String)(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getAttributes($catalog, $schemaPattern, $typeNamePattern, $attributeNamePattern)", _.getAttributes(catalog, schemaPattern, typeNamePattern, attributeNamePattern)), k, resultset.close)

  // TODO: scope
  def getBestRowIdentifier[A](catalog: String, schema: String, table: String, scope: Int, nullable: Boolean)(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getBestRowIdentifier($catalog, $schema, $table, $scope, $nullable)", _.getBestRowIdentifier(catalog, schema, table, scope, nullable)), k, resultset.close)

  def getCatalogs[A](k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getCatalogs", _.getCatalogs), k, resultset.close)

  def getCatalogSeparator: Action[String] =
    primitive(s"getCatalogSeparator", _.getCatalogSeparator)

  def getCatalogTerm: Action[String] =
    primitive(s"getCatalogTerm", _.getCatalogTerm)

  def getClientInfoProperties[A](k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getClientInfoProperties", _.getClientInfoProperties), k, resultset.close)

  def getColumnPrivileges[A](catalog: String, schema: String, table: String, columnNamePattern: String)(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getColumnPrivileges($catalog, $schema, $table, $columnNamePattern)", _.getColumnPrivileges(catalog, schema, table, columnNamePattern)), k, resultset.close)

  def getColumns[A](catalog: String, schemaPattern: String, tableNamePattern: String, columnNamePattern: String)(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getColumns($catalog, $schemaPattern, $tableNamePattern, $columnNamePattern)", _.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern)), k, resultset.close)

  def getConnection[A](k: Action0[sql.Connection, A]): Action[A] =
    gosub0(primitive(s"getConnection", _.getConnection), k)

  def getCrossReference[A](parentCatalog: String, parentSchema: String, parentTable: String, foreignCatalog: String, foreignSchema: String, foreignTable: String)(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getCrossReference($parentCatalog, $parentSchema, $parentTable, $foreignCatalog, $foreignSchema, $foreignTable)", _.getCrossReference(parentCatalog, parentSchema, parentTable, foreignCatalog, foreignSchema, foreignTable)), k, resultset.close)

  def getDatabaseMajorVersion: Action[Int] =
    primitive(s"getDatabaseMajorVersion", _.getDatabaseMajorVersion)

  def getDatabaseMinorVersion: Action[Int] =
    primitive(s"getDatabaseMinorVersion", _.getDatabaseMinorVersion)

  def getDatabaseProductName: Action[String] =
    primitive(s"getDatabaseProductName", _.getDatabaseProductName)

  def getDatabaseProductVersion: Action[String] =
    primitive(s"getDatabaseProductVersion", _.getDatabaseProductVersion)

  def getDefaultTransactionIsolation: Action[Int] =
    primitive(s"getDefaultTransactionIsolation", _.getDefaultTransactionIsolation)

  def getDriverMajorVersion: Action[Int] =
    primitive(s"getDriverMajorVersion", _.getDriverMajorVersion)

  def getDriverMinorVersion: Action[Int] =
    primitive(s"getDriverMinorVersion", _.getDriverMinorVersion)

  def getDriverName: Action[String] =
    primitive(s"getDriverName", _.getDriverName)

  def getDriverVersion: Action[String] =
    primitive(s"getDriverVersion", _.getDriverVersion)

  def getExportedKeys[A](catalog: String, schema: String, table: String)(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getExportedKeys($catalog, $schema, $table)", _.getExportedKeys(catalog, schema, table)), k, resultset.close)

  def getExtraNameCharacters: Action[String] =
    primitive(s"getExtraNameCharacters", _.getExtraNameCharacters)

  def getFunctionColumns[A](catalog: String, schemaPattern: String, functionNamePattern: String, columnNamePattern: String)(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getFunctionColumns($catalog, $schemaPattern, $functionNamePattern, $columnNamePattern)", _.getFunctionColumns(catalog, schemaPattern, functionNamePattern, columnNamePattern)), k, resultset.close)

  def getFunctions[A](catalog: String, schemaPattern: String, functionNamePattern: String)(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getFunctions($catalog, $schemaPattern, $functionNamePattern)", _.getFunctions(catalog, schemaPattern, functionNamePattern)), k, resultset.close)

  def getIdentifierQuoteString: Action[String] =
    primitive(s"getIdentifierQuoteString", _.getIdentifierQuoteString)

  def getImportedKeys[A](catalog: String, schema: String, table: String)(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getImportedKeys($catalog, $schema, $table)", _.getImportedKeys(catalog, schema, table)), k, resultset.close)

  def getIndexInfo[A](catalog: String, schema: String, table: String, unique: Boolean, approximate: Boolean)(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getIndexInfo($catalog, $schema, $table, $unique, $approximate)", _.getIndexInfo(catalog, schema, table, unique, approximate)), k, resultset.close)

  def getJDBCMajorVersion: Action[Int] =
    primitive(s"getJDBCMajorVersion", _.getJDBCMajorVersion)

  def getJDBCMinorVersion: Action[Int] =
    primitive(s"getJDBCMinorVersion", _.getJDBCMinorVersion)

  def getMaxBinaryLiteralLength: Action[Int] =
    primitive(s"getMaxBinaryLiteralLength", _.getMaxBinaryLiteralLength)

  def getMaxCatalogNameLength: Action[Int] =
    primitive(s"getMaxCatalogNameLength", _.getMaxCatalogNameLength)

  def getMaxCharLiteralLength: Action[Int] =
    primitive(s"getMaxCharLiteralLength", _.getMaxCharLiteralLength)

  def getMaxColumnNameLength: Action[Int] =
    primitive(s"getMaxColumnNameLength", _.getMaxColumnNameLength)

  def getMaxColumnsInGroupBy: Action[Int] =
    primitive(s"getMaxColumnsInGroupBy", _.getMaxColumnsInGroupBy)

  def getMaxColumnsInIndex: Action[Int] =
    primitive(s"getMaxColumnsInIndex", _.getMaxColumnsInIndex)

  def getMaxColumnsInOrderBy: Action[Int] =
    primitive(s"getMaxColumnsInOrderBy", _.getMaxColumnsInOrderBy)

  def getMaxColumnsInSelect: Action[Int] =
    primitive(s"getMaxColumnsInSelect", _.getMaxColumnsInSelect)

  def getMaxColumnsInTable: Action[Int] =
    primitive(s"getMaxColumnsInTable", _.getMaxColumnsInTable)

  def getMaxConnections: Action[Int] =
    primitive(s"getMaxConnections", _.getMaxConnections)

  def getMaxCursorNameLength: Action[Int] =
    primitive(s"getMaxCursorNameLength", _.getMaxCursorNameLength)

  def getMaxIndexLength: Action[Int] =
    primitive(s"getMaxIndexLength", _.getMaxIndexLength)

  def getMaxProcedureNameLength: Action[Int] =
    primitive(s"getMaxProcedureNameLength", _.getMaxProcedureNameLength)

  def getMaxRowSize: Action[Int] =
    primitive(s"getMaxRowSize", _.getMaxRowSize)

  def getMaxSchemaNameLength: Action[Int] =
    primitive(s"getMaxSchemaNameLength", _.getMaxSchemaNameLength)

  def getMaxStatementLength: Action[Int] =
    primitive(s"getMaxStatementLength", _.getMaxStatementLength)

  def getMaxStatements: Action[Int] =
    primitive(s"getMaxStatements", _.getMaxStatements)

  def getMaxTableNameLength: Action[Int] =
    primitive(s"getMaxTableNameLength", _.getMaxTableNameLength)

  def getMaxTablesInSelect: Action[Int] =
    primitive(s"getMaxTablesInSelect", _.getMaxTablesInSelect)

  def getMaxUserNameLength: Action[Int] =
    primitive(s"getMaxUserNameLength", _.getMaxUserNameLength)

  def getNumericFunctions: Action[String] =
    primitive(s"getNumericFunctions", _.getNumericFunctions)

  def getPrimaryKeys[A](catalog: String, schema: String, table: String)(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getPrimaryKeys($catalog, $schema, $table)", _.getPrimaryKeys(catalog, schema, table)), k, resultset.close)

  def getProcedureColumns[A](catalog: String, schemaPattern: String, procedureNamePattern: String, columnNamePattern: String)(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getProcedureColumns($catalog, $schemaPattern, $procedureNamePattern, $columnNamePattern)", _.getProcedureColumns(catalog, schemaPattern, procedureNamePattern, columnNamePattern)), k, resultset.close)

  def getProcedures[A](catalog: String, schemaPattern: String, procedureNamePattern: String)(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getProcedures($catalog, $schemaPattern, $procedureNamePattern)", _.getProcedures(catalog, schemaPattern, procedureNamePattern)), k, resultset.close)

  def getProcedureTerm: Action[String] =
    primitive(s"getProcedureTerm", _.getProcedureTerm)

  def getResultSetHoldability: Action[Holdability] =
    primitive(s"getResultSetHoldability", _.getResultSetHoldability).map(Holdability.unsafeFromInt)

  def getRowIdLifetime: Action[RowIdLifetime] =
    primitive(s"getRowIdLifetime", _.getRowIdLifetime)

  def getSchemas[A](k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getSchemas", _.getSchemas), k, resultset.close)

  def getSchemas[A](catalog: String, schemaPattern: String)(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getSchemas($catalog, $schemaPattern)", _.getSchemas(catalog, schemaPattern)), k, resultset.close)

  def getSchemaTerm: Action[String] =
    primitive(s"getSchemaTerm", _.getSchemaTerm)

  def getSearchStringEscape: Action[String] =
    primitive(s"getSearchStringEscape", _.getSearchStringEscape)

  def getSQLKeywords: Action[String] =
    primitive(s"getSQLKeywords", _.getSQLKeywords)

  def getSQLStateType: Action[Int] =
    primitive(s"getSQLStateType", _.getSQLStateType)

  def getStringFunctions: Action[String] =
    primitive(s"getStringFunctions", _.getStringFunctions)

  def getSuperTables[A](catalog: String, schemaPattern: String, tableNamePattern: String)(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getSuperTables($catalog, $schemaPattern, $tableNamePattern)", _.getSuperTables(catalog, schemaPattern, tableNamePattern)), k, resultset.close)

  def getSuperTypes[A](catalog: String, schemaPattern: String, typeNamePattern: String)(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getSuperTypes($catalog, $schemaPattern, $typeNamePattern)", _.getSuperTypes(catalog, schemaPattern, typeNamePattern)), k, resultset.close)

  def getSystemFunctions: Action[String] =
    primitive(s"getSystemFunctions", _.getSystemFunctions)

  def getTablePrivileges[A](catalog: String, schemaPattern: String, tableNamePattern: String)(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getTablePrivileges($catalog, $schemaPattern, $tableNamePattern)", _.getTablePrivileges(catalog, schemaPattern, tableNamePattern)), k, resultset.close)

  def getTables[A](catalog: String, schemaPattern: String, tableNamePattern: String, types: Seq[String])(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getTables($catalog, $schemaPattern, $tableNamePattern, $types)", _.getTables(catalog, schemaPattern, tableNamePattern, types.toArray)), k, resultset.close)

  def getTableTypes[A](k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getTableTypes", _.getTableTypes), k, resultset.close)

  def getTimeDateFunctions: Action[String] =
    primitive(s"getTimeDateFunctions", _.getTimeDateFunctions)

  def getTypeInfo[A](k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getTypeInfo", _.getTypeInfo), k, resultset.close)

  def getUDTs[A](catalog: String, schemaPattern: String, typeNamePattern: String, types: Seq[JdbcType])(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getUDTs($catalog, $schemaPattern, $typeNamePattern, $types)", _.getUDTs(catalog, schemaPattern, typeNamePattern, types.map(_.toInt).toArray)), k, resultset.close)

  def getURL: Action[String] =
    primitive(s"getURL", _.getURL)

  def getUserName: Action[String] =
    primitive(s"getUserName", _.getUserName)

  def getVersionColumns[A](catalog: String, schema: String, table: String)(k: Action0[sql.ResultSet, A]): Action[A] =
    gosub(primitive(s"getVersionColumns($catalog, $schema, $table)", _.getVersionColumns(catalog, schema, table)), k, resultset.close)

  // todo: enum
  def insertsAreDetected(kind: Int): Action[Boolean] =
    primitive(s"insertsAreDetected($kind)", _.insertsAreDetected(kind))

  def isCatalogAtStart: Action[Boolean] =
    primitive(s"isCatalogAtStart", _.isCatalogAtStart)

  def isReadOnly: Action[Boolean] =
    primitive(s"isReadOnly", _.isReadOnly)

  def locatorsUpdateCopy: Action[Boolean] =
    primitive(s"locatorsUpdateCopy", _.locatorsUpdateCopy)

  def nullPlusNonNullIsNull: Action[Boolean] =
    primitive(s"nullPlusNonNullIsNull", _.nullPlusNonNullIsNull)

  def nullsAreSortedAtEnd: Action[Boolean] =
    primitive(s"nullsAreSortedAtEnd", _.nullsAreSortedAtEnd)

  def nullsAreSortedAtStart: Action[Boolean] =
    primitive(s"nullsAreSortedAtStart", _.nullsAreSortedAtStart)

  def nullsAreSortedHigh: Action[Boolean] =
    primitive(s"nullsAreSortedHigh", _.nullsAreSortedHigh)

  def nullsAreSortedLow: Action[Boolean] =
    primitive(s"nullsAreSortedLow", _.nullsAreSortedLow)

  // todo: enum
  def othersDeletesAreVisible(kind: Int): Action[Boolean] =
    primitive(s"othersDeletesAreVisible($kind)", _.othersDeletesAreVisible(kind))

  // todo: enum
  def othersInsertsAreVisible(kind: Int): Action[Boolean] =
    primitive(s"othersInsertsAreVisible($kind)", _.othersInsertsAreVisible(kind))

  // todo: enum
  def othersUpdatesAreVisible(kind: Int): Action[Boolean] =
    primitive(s"othersUpdatesAreVisible($kind)", _.othersUpdatesAreVisible(kind))

  // todo: enum
  def ownDeletesAreVisible(kind: Int): Action[Boolean] =
    primitive(s"ownDeletesAreVisible($kind)", _.ownDeletesAreVisible(kind))

  // todo: enum
  def ownInsertsAreVisible(kind: Int): Action[Boolean] =
    primitive(s"ownInsertsAreVisible($kind)", _.ownInsertsAreVisible(kind))

  // todo: enum
  def ownUpdatesAreVisible(kind: Int): Action[Boolean] =
    primitive(s"ownUpdatesAreVisible($kind)", _.ownUpdatesAreVisible(kind))

  def storesLowerCaseIdentifiers: Action[Boolean] =
    primitive(s"storesLowerCaseIdentifiers", _.storesLowerCaseIdentifiers)

  def storesLowerCaseQuotedIdentifiers: Action[Boolean] =
    primitive(s"storesLowerCaseQuotedIdentifiers", _.storesLowerCaseQuotedIdentifiers)

  def storesMixedCaseIdentifiers: Action[Boolean] =
    primitive(s"storesMixedCaseIdentifiers", _.storesMixedCaseIdentifiers)

  def storesMixedCaseQuotedIdentifiers: Action[Boolean] =
    primitive(s"storesMixedCaseQuotedIdentifiers", _.storesMixedCaseQuotedIdentifiers)

  def storesUpperCaseIdentifiers: Action[Boolean] =
    primitive(s"storesUpperCaseIdentifiers", _.storesUpperCaseIdentifiers)

  def storesUpperCaseQuotedIdentifiers: Action[Boolean] =
    primitive(s"storesUpperCaseQuotedIdentifiers", _.storesUpperCaseQuotedIdentifiers)

  def supportsAlterTableWithAddColumn: Action[Boolean] =
    primitive(s"supportsAlterTableWithAddColumn", _.supportsAlterTableWithAddColumn)

  def supportsAlterTableWithDropColumn: Action[Boolean] =
    primitive(s"supportsAlterTableWithDropColumn", _.supportsAlterTableWithDropColumn)

  def supportsANSI92EntryLevelSQL: Action[Boolean] =
    primitive(s"supportsANSI92EntryLevelSQL", _.supportsANSI92EntryLevelSQL)

  def supportsANSI92FullSQL: Action[Boolean] =
    primitive(s"supportsANSI92FullSQL", _.supportsANSI92FullSQL)

  def supportsANSI92IntermediateSQL: Action[Boolean] =
    primitive(s"supportsANSI92IntermediateSQL", _.supportsANSI92IntermediateSQL)

  def supportsBatchUpdates: Action[Boolean] =
    primitive(s"supportsBatchUpdates", _.supportsBatchUpdates)

  def supportsCatalogsInDataManipulation: Action[Boolean] =
    primitive(s"supportsCatalogsInDataManipulation", _.supportsCatalogsInDataManipulation)

  def supportsCatalogsInIndexDefinitions: Action[Boolean] =
    primitive(s"supportsCatalogsInIndexDefinitions", _.supportsCatalogsInIndexDefinitions)

  def supportsCatalogsInPrivilegeDefinitions: Action[Boolean] =
    primitive(s"supportsCatalogsInPrivilegeDefinitions", _.supportsCatalogsInPrivilegeDefinitions)

  def supportsCatalogsInProcedureCalls: Action[Boolean] =
    primitive(s"supportsCatalogsInProcedureCalls", _.supportsCatalogsInProcedureCalls)

  def supportsCatalogsInTableDefinitions: Action[Boolean] =
    primitive(s"supportsCatalogsInTableDefinitions", _.supportsCatalogsInTableDefinitions)

  def supportsColumnAliasing: Action[Boolean] =
    primitive(s"supportsColumnAliasing", _.supportsColumnAliasing)

  def supportsConvert: Action[Boolean] =
    primitive(s"supportsConvert", _.supportsConvert)

  def supportsConvert(fromType: JdbcType, toType: JdbcType): Action[Boolean] =
    primitive(s"supportsConvert($fromType, $toType)", _.supportsConvert(fromType.toInt, toType.toInt))

  def supportsCoreSQLGrammar: Action[Boolean] =
    primitive(s"supportsCoreSQLGrammar", _.supportsCoreSQLGrammar)

  def supportsCorrelatedSubqueries: Action[Boolean] =
    primitive(s"supportsCorrelatedSubqueries", _.supportsCorrelatedSubqueries)

  def supportsDataDefinitionAndDataManipulationTransactions: Action[Boolean] =
    primitive(s"supportsDataDefinitionAndDataManipulationTransactions", _.supportsDataDefinitionAndDataManipulationTransactions)

  def supportsDataManipulationTransactionsOnly: Action[Boolean] =
    primitive(s"supportsDataManipulationTransactionsOnly", _.supportsDataManipulationTransactionsOnly)

  def supportsDifferentTableCorrelationNames: Action[Boolean] =
    primitive(s"supportsDifferentTableCorrelationNames", _.supportsDifferentTableCorrelationNames)

  def supportsExpressionsInOrderBy: Action[Boolean] =
    primitive(s"supportsExpressionsInOrderBy", _.supportsExpressionsInOrderBy)

  def supportsExtendedSQLGrammar: Action[Boolean] =
    primitive(s"supportsExtendedSQLGrammar", _.supportsExtendedSQLGrammar)

  def supportsFullOuterJoins: Action[Boolean] =
    primitive(s"supportsFullOuterJoins", _.supportsFullOuterJoins)

  def supportsGetGeneratedKeys: Action[Boolean] =
    primitive(s"supportsGetGeneratedKeys", _.supportsGetGeneratedKeys)

  def supportsGroupBy: Action[Boolean] =
    primitive(s"supportsGroupBy", _.supportsGroupBy)

  def supportsGroupByBeyondSelect: Action[Boolean] =
    primitive(s"supportsGroupByBeyondSelect", _.supportsGroupByBeyondSelect)

  def supportsGroupByUnrelated: Action[Boolean] =
    primitive(s"supportsGroupByUnrelated", _.supportsGroupByUnrelated)

  def supportsIntegrityEnhancementFacility: Action[Boolean] =
    primitive(s"supportsIntegrityEnhancementFacility", _.supportsIntegrityEnhancementFacility)

  def supportsLikeEscapeClause: Action[Boolean] =
    primitive(s"supportsLikeEscapeClause", _.supportsLikeEscapeClause)

  def supportsLimitedOuterJoins: Action[Boolean] =
    primitive(s"supportsLimitedOuterJoins", _.supportsLimitedOuterJoins)

  def supportsMinimumSQLGrammar: Action[Boolean] =
    primitive(s"supportsMinimumSQLGrammar", _.supportsMinimumSQLGrammar)

  def supportsMixedCaseIdentifiers: Action[Boolean] =
    primitive(s"supportsMixedCaseIdentifiers", _.supportsMixedCaseIdentifiers)

  def supportsMixedCaseQuotedIdentifiers: Action[Boolean] =
    primitive(s"supportsMixedCaseQuotedIdentifiers", _.supportsMixedCaseQuotedIdentifiers)

  def supportsMultipleOpenResults: Action[Boolean] =
    primitive(s"supportsMultipleOpenResults", _.supportsMultipleOpenResults)

  def supportsMultipleResultSets: Action[Boolean] =
    primitive(s"supportsMultipleResultSets", _.supportsMultipleResultSets)

  def supportsMultipleTransactions: Action[Boolean] =
    primitive(s"supportsMultipleTransactions", _.supportsMultipleTransactions)

  def supportsNamedParameters: Action[Boolean] =
    primitive(s"supportsNamedParameters", _.supportsNamedParameters)

  def supportsNonNullableColumns: Action[Boolean] =
    primitive(s"supportsNonNullableColumns", _.supportsNonNullableColumns)

  def supportsOpenCursorsAcrossCommit: Action[Boolean] =
    primitive(s"supportsOpenCursorsAcrossCommit", _.supportsOpenCursorsAcrossCommit)

  def supportsOpenCursorsAcrossRollback: Action[Boolean] =
    primitive(s"supportsOpenCursorsAcrossRollback", _.supportsOpenCursorsAcrossRollback)

  def supportsOpenStatementsAcrossCommit: Action[Boolean] =
    primitive(s"supportsOpenStatementsAcrossCommit", _.supportsOpenStatementsAcrossCommit)

  def supportsOpenStatementsAcrossRollback: Action[Boolean] =
    primitive(s"supportsOpenStatementsAcrossRollback", _.supportsOpenStatementsAcrossRollback)

  def supportsOrderByUnrelated: Action[Boolean] =
    primitive(s"supportsOrderByUnrelated", _.supportsOrderByUnrelated)

  def supportsOuterJoins: Action[Boolean] =
    primitive(s"supportsOuterJoins", _.supportsOuterJoins)

  def supportsPositionedDelete: Action[Boolean] =
    primitive(s"supportsPositionedDelete", _.supportsPositionedDelete)

  def supportsPositionedUpdate: Action[Boolean] =
    primitive(s"supportsPositionedUpdate", _.supportsPositionedUpdate)

  // todo: enum
  def supportsResultSetConcurrency(kind: Int, concurrency: Int): Action[Boolean] =
    primitive(s"supportsResultSetConcurrency($kind, $concurrency)", _.supportsResultSetConcurrency(kind, concurrency))

  def supportsResultSetHoldability(holdability: Holdability): Action[Boolean] =
    primitive(s"supportsResultSetHoldability($holdability)", _.supportsResultSetHoldability(holdability.toInt))

  def supportsResultSetType(kind: ResultSetType): Action[Boolean] =
    primitive(s"supportsResultSetType($kind)", _.supportsResultSetType(kind.toInt))

  def supportsSavepoInts: Action[Boolean] =
    primitive(s"supportsSavepoInts", _.supportsSavepoints)

  def supportsSchemasInDataManipulation: Action[Boolean] =
    primitive(s"supportsSchemasInDataManipulation", _.supportsSchemasInDataManipulation)

  def supportsSchemasInIndexDefinitions: Action[Boolean] =
    primitive(s"supportsSchemasInIndexDefinitions", _.supportsSchemasInIndexDefinitions)

  def supportsSchemasInPrivilegeDefinitions: Action[Boolean] =
    primitive(s"supportsSchemasInPrivilegeDefinitions", _.supportsSchemasInPrivilegeDefinitions)

  def supportsSchemasInProcedureCalls: Action[Boolean] =
    primitive(s"supportsSchemasInProcedureCalls", _.supportsSchemasInProcedureCalls)

  def supportsSchemasInTableDefinitions: Action[Boolean] =
    primitive(s"supportsSchemasInTableDefinitions", _.supportsSchemasInTableDefinitions)

  def supportsSelectForUpdate: Action[Boolean] =
    primitive(s"supportsSelectForUpdate", _.supportsSelectForUpdate)

  def supportsStatementPooling: Action[Boolean] =
    primitive(s"supportsStatementPooling", _.supportsStatementPooling)

  def supportsStoredFunctionsUsingCallSyntax: Action[Boolean] =
    primitive(s"supportsStoredFunctionsUsingCallSyntax", _.supportsStoredFunctionsUsingCallSyntax)

  def supportsStoredProcedures: Action[Boolean] =
    primitive(s"supportsStoredProcedures", _.supportsStoredProcedures)

  def supportsSubqueriesInComparisons: Action[Boolean] =
    primitive(s"supportsSubqueriesInComparisons", _.supportsSubqueriesInComparisons)

  def supportsSubqueriesInExists: Action[Boolean] =
    primitive(s"supportsSubqueriesInExists", _.supportsSubqueriesInExists)

  def supportsSubqueriesInIns: Action[Boolean] =
    primitive(s"supportsSubqueriesInIns", _.supportsSubqueriesInIns)

  def supportsSubqueriesInQuantifieds: Action[Boolean] =
    primitive(s"supportsSubqueriesInQuantifieds", _.supportsSubqueriesInQuantifieds)

  def supportsTableCorrelationNames: Action[Boolean] =
    primitive(s"supportsTableCorrelationNames", _.supportsTableCorrelationNames)

  def supportsTransactionIsolationLevel(level: TransactionIsolation): Action[Boolean] =
    primitive(s"supportsTransactionIsolationLevel(level)", _.supportsTransactionIsolationLevel(level.toInt))

  def supportsTransactions: Action[Boolean] =
    primitive(s"supportsTransactions", _.supportsTransactions)

  def supportsUnion: Action[Boolean] =
    primitive(s"supportsUnion", _.supportsUnion)

  def supportsUnionAll: Action[Boolean] =
    primitive(s"supportsUnionAll", _.supportsUnionAll)

  // todo: enum
  def updatesAreDetected(kind: Int): Action[Boolean] =
    primitive(s"updatesAreDetected($kind)", _.updatesAreDetected(kind))

  def usesLocalFilePerTable: Action[Boolean] =
    primitive(s"usesLocalFilePerTable", _.usesLocalFilePerTable)

  def usesLocalFiles: Action[Boolean] =
    primitive(s"usesLocalFiles", _.usesLocalFiles)

}