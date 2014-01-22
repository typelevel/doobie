package doobie
package dbc

import scalaz.effect.IO
import java.sql
import java.sql._

// TODO: review enum usage
trait DatabaseMetaDataFunctions extends DWorld[sql.DatabaseMetaData] {

  def allProceduresAreCallable: DatabaseMetaData[Boolean] =
    primitive(s"allProceduresAreCallable", _.allProceduresAreCallable)

  def allTablesAreSelectable: DatabaseMetaData[Boolean] =
    primitive(s"allTablesAreSelectable", _.allTablesAreSelectable)

  def autoCommitFailureClosesAllResultSets: DatabaseMetaData[Boolean] =
    primitive(s"autoCommitFailureClosesAllResultSets", _.autoCommitFailureClosesAllResultSets)

  def dataDefinitionCausesTransactionCommit: DatabaseMetaData[Boolean] =
    primitive(s"dataDefinitionCausesTransactionCommit", _.dataDefinitionCausesTransactionCommit)

  def dataDefinitionIgnoredInTransactions: DatabaseMetaData[Boolean] =
    primitive(s"dataDefinitionIgnoredInTransactions", _.dataDefinitionIgnoredInTransactions)

  def deletesAreDetected(kind: Int): DatabaseMetaData[Boolean] =
    primitive(s"deletesAreDetected($kind)", _.deletesAreDetected(kind))

  def doesMaxRowSizeIncludeBlobs: DatabaseMetaData[Boolean] =
    primitive(s"doesMaxRowSizeIncludeBlobs", _.doesMaxRowSizeIncludeBlobs)

  def getAttributes[A](catalog: String, schemaPattern: String, typeNamePattern: String, attributeNamePattern: String)(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getAttributes($catalog, $schemaPattern, $typeNamePattern, $attributeNamePattern)", _.getAttributes(catalog, schemaPattern, typeNamePattern, attributeNamePattern)), k, resultset.close)

  // TODO: scope
  def getBestRowIdentifier[A](catalog: String, schema: String, table: String, scope: Int, nullable: Boolean)(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getBestRowIdentifier($catalog, $schema, $table, $scope, $nullable)", _.getBestRowIdentifier(catalog, schema, table, scope, nullable)), k, resultset.close)

  def getCatalogs[A](k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getCatalogs", _.getCatalogs), k, resultset.close)

  def getCatalogSeparator: DatabaseMetaData[String] =
    primitive(s"getCatalogSeparator", _.getCatalogSeparator)

  def getCatalogTerm: DatabaseMetaData[String] =
    primitive(s"getCatalogTerm", _.getCatalogTerm)

  def getClientInfoProperties[A](k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getClientInfoProperties", _.getClientInfoProperties), k, resultset.close)

  def getColumnPrivileges[A](catalog: String, schema: String, table: String, columnNamePattern: String)(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getColumnPrivileges($catalog, $schema, $table, $columnNamePattern)", _.getColumnPrivileges(catalog, schema, table, columnNamePattern)), k, resultset.close)

  def getColumns[A](catalog: String, schemaPattern: String, tableNamePattern: String, columnNamePattern: String)(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getColumns($catalog, $schemaPattern, $tableNamePattern, $columnNamePattern)", _.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern)), k, resultset.close)

  def getConnection[A](k: Connection[A]): DatabaseMetaData[A] =
    gosub0(primitive(s"getConnection", _.getConnection), k)

  def getCrossReference[A](parentCatalog: String, parentSchema: String, parentTable: String, foreignCatalog: String, foreignSchema: String, foreignTable: String)(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getCrossReference($parentCatalog, $parentSchema, $parentTable, $foreignCatalog, $foreignSchema, $foreignTable)", _.getCrossReference(parentCatalog, parentSchema, parentTable, foreignCatalog, foreignSchema, foreignTable)), k, resultset.close)

  def getDatabaseMajorVersion: DatabaseMetaData[Int] =
    primitive(s"getDatabaseMajorVersion", _.getDatabaseMajorVersion)

  def getDatabaseMinorVersion: DatabaseMetaData[Int] =
    primitive(s"getDatabaseMinorVersion", _.getDatabaseMinorVersion)

  def getDatabaseProductName: DatabaseMetaData[String] =
    primitive(s"getDatabaseProductName", _.getDatabaseProductName)

  def getDatabaseProductVersion: DatabaseMetaData[String] =
    primitive(s"getDatabaseProductVersion", _.getDatabaseProductVersion)

  def getDefaultTransactionIsolation: DatabaseMetaData[Int] =
    primitive(s"getDefaultTransactionIsolation", _.getDefaultTransactionIsolation)

  def getDriverMajorVersion: DatabaseMetaData[Int] =
    primitive(s"getDriverMajorVersion", _.getDriverMajorVersion)

  def getDriverMinorVersion: DatabaseMetaData[Int] =
    primitive(s"getDriverMinorVersion", _.getDriverMinorVersion)

  def getDriverName: DatabaseMetaData[String] =
    primitive(s"getDriverName", _.getDriverName)

  def getDriverVersion: DatabaseMetaData[String] =
    primitive(s"getDriverVersion", _.getDriverVersion)

  def getExportedKeys[A](catalog: String, schema: String, table: String)(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getExportedKeys($catalog, $schema, $table)", _.getExportedKeys(catalog, schema, table)), k, resultset.close)

  def getExtraNameCharacters: DatabaseMetaData[String] =
    primitive(s"getExtraNameCharacters", _.getExtraNameCharacters)

  def getFunctionColumns[A](catalog: String, schemaPattern: String, functionNamePattern: String, columnNamePattern: String)(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getFunctionColumns($catalog, $schemaPattern, $functionNamePattern, $columnNamePattern)", _.getFunctionColumns(catalog, schemaPattern, functionNamePattern, columnNamePattern)), k, resultset.close)

  def getFunctions[A](catalog: String, schemaPattern: String, functionNamePattern: String)(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getFunctions($catalog, $schemaPattern, $functionNamePattern)", _.getFunctions(catalog, schemaPattern, functionNamePattern)), k, resultset.close)

  def getIdentifierQuoteString: DatabaseMetaData[String] =
    primitive(s"getIdentifierQuoteString", _.getIdentifierQuoteString)

  def getImportedKeys[A](catalog: String, schema: String, table: String)(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getImportedKeys($catalog, $schema, $table)", _.getImportedKeys(catalog, schema, table)), k, resultset.close)

  def getIndexInfo[A](catalog: String, schema: String, table: String, unique: Boolean, approximate: Boolean)(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getIndexInfo($catalog, $schema, $table, $unique, $approximate)", _.getIndexInfo(catalog, schema, table, unique, approximate)), k, resultset.close)

  def getJDBCMajorVersion: DatabaseMetaData[Int] =
    primitive(s"getJDBCMajorVersion", _.getJDBCMajorVersion)

  def getJDBCMinorVersion: DatabaseMetaData[Int] =
    primitive(s"getJDBCMinorVersion", _.getJDBCMinorVersion)

  def getMaxBinaryLiteralLength: DatabaseMetaData[Int] =
    primitive(s"getMaxBinaryLiteralLength", _.getMaxBinaryLiteralLength)

  def getMaxCatalogNameLength: DatabaseMetaData[Int] =
    primitive(s"getMaxCatalogNameLength", _.getMaxCatalogNameLength)

  def getMaxCharLiteralLength: DatabaseMetaData[Int] =
    primitive(s"getMaxCharLiteralLength", _.getMaxCharLiteralLength)

  def getMaxColumnNameLength: DatabaseMetaData[Int] =
    primitive(s"getMaxColumnNameLength", _.getMaxColumnNameLength)

  def getMaxColumnsInGroupBy: DatabaseMetaData[Int] =
    primitive(s"getMaxColumnsInGroupBy", _.getMaxColumnsInGroupBy)

  def getMaxColumnsInIndex: DatabaseMetaData[Int] =
    primitive(s"getMaxColumnsInIndex", _.getMaxColumnsInIndex)

  def getMaxColumnsInOrderBy: DatabaseMetaData[Int] =
    primitive(s"getMaxColumnsInOrderBy", _.getMaxColumnsInOrderBy)

  def getMaxColumnsInSelect: DatabaseMetaData[Int] =
    primitive(s"getMaxColumnsInSelect", _.getMaxColumnsInSelect)

  def getMaxColumnsInTable: DatabaseMetaData[Int] =
    primitive(s"getMaxColumnsInTable", _.getMaxColumnsInTable)

  def getMaxConnections: DatabaseMetaData[Int] =
    primitive(s"getMaxConnections", _.getMaxConnections)

  def getMaxCursorNameLength: DatabaseMetaData[Int] =
    primitive(s"getMaxCursorNameLength", _.getMaxCursorNameLength)

  def getMaxIndexLength: DatabaseMetaData[Int] =
    primitive(s"getMaxIndexLength", _.getMaxIndexLength)

  def getMaxProcedureNameLength: DatabaseMetaData[Int] =
    primitive(s"getMaxProcedureNameLength", _.getMaxProcedureNameLength)

  def getMaxRowSize: DatabaseMetaData[Int] =
    primitive(s"getMaxRowSize", _.getMaxRowSize)

  def getMaxSchemaNameLength: DatabaseMetaData[Int] =
    primitive(s"getMaxSchemaNameLength", _.getMaxSchemaNameLength)

  def getMaxStatementLength: DatabaseMetaData[Int] =
    primitive(s"getMaxStatementLength", _.getMaxStatementLength)

  def getMaxStatements: DatabaseMetaData[Int] =
    primitive(s"getMaxStatements", _.getMaxStatements)

  def getMaxTableNameLength: DatabaseMetaData[Int] =
    primitive(s"getMaxTableNameLength", _.getMaxTableNameLength)

  def getMaxTablesInSelect: DatabaseMetaData[Int] =
    primitive(s"getMaxTablesInSelect", _.getMaxTablesInSelect)

  def getMaxUserNameLength: DatabaseMetaData[Int] =
    primitive(s"getMaxUserNameLength", _.getMaxUserNameLength)

  def getNumericFunctions: DatabaseMetaData[String] =
    primitive(s"getNumericFunctions", _.getNumericFunctions)

  def getPrimaryKeys[A](catalog: String, schema: String, table: String)(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getPrimaryKeys($catalog, $schema, $table)", _.getPrimaryKeys(catalog, schema, table)), k, resultset.close)

  def getProcedureColumns[A](catalog: String, schemaPattern: String, procedureNamePattern: String, columnNamePattern: String)(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getProcedureColumns($catalog, $schemaPattern, $procedureNamePattern, $columnNamePattern)", _.getProcedureColumns(catalog, schemaPattern, procedureNamePattern, columnNamePattern)), k, resultset.close)

  def getProcedures[A](catalog: String, schemaPattern: String, procedureNamePattern: String)(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getProcedures($catalog, $schemaPattern, $procedureNamePattern)", _.getProcedures(catalog, schemaPattern, procedureNamePattern)), k, resultset.close)

  def getProcedureTerm: DatabaseMetaData[String] =
    primitive(s"getProcedureTerm", _.getProcedureTerm)

  def getResultSetHoldability: DatabaseMetaData[Holdability] =
    primitive(s"getResultSetHoldability", _.getResultSetHoldability).map(Holdability.unsafeFromInt)

  def getRowIdLifetime: DatabaseMetaData[RowIdLifetime] =
    primitive(s"getRowIdLifetime", _.getRowIdLifetime)

  def getSchemas[A](k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getSchemas", _.getSchemas), k, resultset.close)

  def getSchemas[A](catalog: String, schemaPattern: String)(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getSchemas($catalog, $schemaPattern)", _.getSchemas(catalog, schemaPattern)), k, resultset.close)

  def getSchemaTerm: DatabaseMetaData[String] =
    primitive(s"getSchemaTerm", _.getSchemaTerm)

  def getSearchStringEscape: DatabaseMetaData[String] =
    primitive(s"getSearchStringEscape", _.getSearchStringEscape)

  def getSQLKeywords: DatabaseMetaData[String] =
    primitive(s"getSQLKeywords", _.getSQLKeywords)

  def getSQLStateType: DatabaseMetaData[Int] =
    primitive(s"getSQLStateType", _.getSQLStateType)

  def getStringFunctions: DatabaseMetaData[String] =
    primitive(s"getStringFunctions", _.getStringFunctions)

  def getSuperTables[A](catalog: String, schemaPattern: String, tableNamePattern: String)(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getSuperTables($catalog, $schemaPattern, $tableNamePattern)", _.getSuperTables(catalog, schemaPattern, tableNamePattern)), k, resultset.close)

  def getSuperTypes[A](catalog: String, schemaPattern: String, typeNamePattern: String)(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getSuperTypes($catalog, $schemaPattern, $typeNamePattern)", _.getSuperTypes(catalog, schemaPattern, typeNamePattern)), k, resultset.close)

  def getSystemFunctions: DatabaseMetaData[String] =
    primitive(s"getSystemFunctions", _.getSystemFunctions)

  def getTablePrivileges[A](catalog: String, schemaPattern: String, tableNamePattern: String)(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getTablePrivileges($catalog, $schemaPattern, $tableNamePattern)", _.getTablePrivileges(catalog, schemaPattern, tableNamePattern)), k, resultset.close)

  def getTables[A](catalog: String, schemaPattern: String, tableNamePattern: String, types: Seq[String])(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getTables($catalog, $schemaPattern, $tableNamePattern, $types)", _.getTables(catalog, schemaPattern, tableNamePattern, types.toArray)), k, resultset.close)

  def getTableTypes[A](k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getTableTypes", _.getTableTypes), k, resultset.close)

  def getTimeDateFunctions: DatabaseMetaData[String] =
    primitive(s"getTimeDateFunctions", _.getTimeDateFunctions)

  def getTypeInfo[A](k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getTypeInfo", _.getTypeInfo), k, resultset.close)

  def getUDTs[A](catalog: String, schemaPattern: String, typeNamePattern: String, types: Seq[JdbcType])(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getUDTs($catalog, $schemaPattern, $typeNamePattern, $types)", _.getUDTs(catalog, schemaPattern, typeNamePattern, types.map(_.toInt).toArray)), k, resultset.close)

  def getURL: DatabaseMetaData[String] =
    primitive(s"getURL", _.getURL)

  def getUserName: DatabaseMetaData[String] =
    primitive(s"getUserName", _.getUserName)

  def getVersionColumns[A](catalog: String, schema: String, table: String)(k: ResultSet[A]): DatabaseMetaData[A] =
    gosub(primitive(s"getVersionColumns($catalog, $schema, $table)", _.getVersionColumns(catalog, schema, table)), k, resultset.close)

  // todo: enum
  def insertsAreDetected(kind: Int): DatabaseMetaData[Boolean] =
    primitive(s"insertsAreDetected($kind)", _.insertsAreDetected(kind))

  def isCatalogAtStart: DatabaseMetaData[Boolean] =
    primitive(s"isCatalogAtStart", _.isCatalogAtStart)

  def isReadOnly: DatabaseMetaData[Boolean] =
    primitive(s"isReadOnly", _.isReadOnly)

  def locatorsUpdateCopy: DatabaseMetaData[Boolean] =
    primitive(s"locatorsUpdateCopy", _.locatorsUpdateCopy)

  def nullPlusNonNullIsNull: DatabaseMetaData[Boolean] =
    primitive(s"nullPlusNonNullIsNull", _.nullPlusNonNullIsNull)

  def nullsAreSortedAtEnd: DatabaseMetaData[Boolean] =
    primitive(s"nullsAreSortedAtEnd", _.nullsAreSortedAtEnd)

  def nullsAreSortedAtStart: DatabaseMetaData[Boolean] =
    primitive(s"nullsAreSortedAtStart", _.nullsAreSortedAtStart)

  def nullsAreSortedHigh: DatabaseMetaData[Boolean] =
    primitive(s"nullsAreSortedHigh", _.nullsAreSortedHigh)

  def nullsAreSortedLow: DatabaseMetaData[Boolean] =
    primitive(s"nullsAreSortedLow", _.nullsAreSortedLow)

  // todo: enum
  def othersDeletesAreVisible(kind: Int): DatabaseMetaData[Boolean] =
    primitive(s"othersDeletesAreVisible($kind)", _.othersDeletesAreVisible(kind))

  // todo: enum
  def othersInsertsAreVisible(kind: Int): DatabaseMetaData[Boolean] =
    primitive(s"othersInsertsAreVisible($kind)", _.othersInsertsAreVisible(kind))

  // todo: enum
  def othersUpdatesAreVisible(kind: Int): DatabaseMetaData[Boolean] =
    primitive(s"othersUpdatesAreVisible($kind)", _.othersUpdatesAreVisible(kind))

  // todo: enum
  def ownDeletesAreVisible(kind: Int): DatabaseMetaData[Boolean] =
    primitive(s"ownDeletesAreVisible($kind)", _.ownDeletesAreVisible(kind))

  // todo: enum
  def ownInsertsAreVisible(kind: Int): DatabaseMetaData[Boolean] =
    primitive(s"ownInsertsAreVisible($kind)", _.ownInsertsAreVisible(kind))

  // todo: enum
  def ownUpdatesAreVisible(kind: Int): DatabaseMetaData[Boolean] =
    primitive(s"ownUpdatesAreVisible($kind)", _.ownUpdatesAreVisible(kind))

  def storesLowerCaseIdentifiers: DatabaseMetaData[Boolean] =
    primitive(s"storesLowerCaseIdentifiers", _.storesLowerCaseIdentifiers)

  def storesLowerCaseQuotedIdentifiers: DatabaseMetaData[Boolean] =
    primitive(s"storesLowerCaseQuotedIdentifiers", _.storesLowerCaseQuotedIdentifiers)

  def storesMixedCaseIdentifiers: DatabaseMetaData[Boolean] =
    primitive(s"storesMixedCaseIdentifiers", _.storesMixedCaseIdentifiers)

  def storesMixedCaseQuotedIdentifiers: DatabaseMetaData[Boolean] =
    primitive(s"storesMixedCaseQuotedIdentifiers", _.storesMixedCaseQuotedIdentifiers)

  def storesUpperCaseIdentifiers: DatabaseMetaData[Boolean] =
    primitive(s"storesUpperCaseIdentifiers", _.storesUpperCaseIdentifiers)

  def storesUpperCaseQuotedIdentifiers: DatabaseMetaData[Boolean] =
    primitive(s"storesUpperCaseQuotedIdentifiers", _.storesUpperCaseQuotedIdentifiers)

  def supportsAlterTableWithAddColumn: DatabaseMetaData[Boolean] =
    primitive(s"supportsAlterTableWithAddColumn", _.supportsAlterTableWithAddColumn)

  def supportsAlterTableWithDropColumn: DatabaseMetaData[Boolean] =
    primitive(s"supportsAlterTableWithDropColumn", _.supportsAlterTableWithDropColumn)

  def supportsANSI92EntryLevelSQL: DatabaseMetaData[Boolean] =
    primitive(s"supportsANSI92EntryLevelSQL", _.supportsANSI92EntryLevelSQL)

  def supportsANSI92FullSQL: DatabaseMetaData[Boolean] =
    primitive(s"supportsANSI92FullSQL", _.supportsANSI92FullSQL)

  def supportsANSI92IntermediateSQL: DatabaseMetaData[Boolean] =
    primitive(s"supportsANSI92IntermediateSQL", _.supportsANSI92IntermediateSQL)

  def supportsBatchUpdates: DatabaseMetaData[Boolean] =
    primitive(s"supportsBatchUpdates", _.supportsBatchUpdates)

  def supportsCatalogsInDataManipulation: DatabaseMetaData[Boolean] =
    primitive(s"supportsCatalogsInDataManipulation", _.supportsCatalogsInDataManipulation)

  def supportsCatalogsInIndexDefinitions: DatabaseMetaData[Boolean] =
    primitive(s"supportsCatalogsInIndexDefinitions", _.supportsCatalogsInIndexDefinitions)

  def supportsCatalogsInPrivilegeDefinitions: DatabaseMetaData[Boolean] =
    primitive(s"supportsCatalogsInPrivilegeDefinitions", _.supportsCatalogsInPrivilegeDefinitions)

  def supportsCatalogsInProcedureCalls: DatabaseMetaData[Boolean] =
    primitive(s"supportsCatalogsInProcedureCalls", _.supportsCatalogsInProcedureCalls)

  def supportsCatalogsInTableDefinitions: DatabaseMetaData[Boolean] =
    primitive(s"supportsCatalogsInTableDefinitions", _.supportsCatalogsInTableDefinitions)

  def supportsColumnAliasing: DatabaseMetaData[Boolean] =
    primitive(s"supportsColumnAliasing", _.supportsColumnAliasing)

  def supportsConvert: DatabaseMetaData[Boolean] =
    primitive(s"supportsConvert", _.supportsConvert)

  def supportsConvert(fromType: JdbcType, toType: JdbcType): DatabaseMetaData[Boolean] =
    primitive(s"supportsConvert($fromType, $toType)", _.supportsConvert(fromType.toInt, toType.toInt))

  def supportsCoreSQLGrammar: DatabaseMetaData[Boolean] =
    primitive(s"supportsCoreSQLGrammar", _.supportsCoreSQLGrammar)

  def supportsCorrelatedSubqueries: DatabaseMetaData[Boolean] =
    primitive(s"supportsCorrelatedSubqueries", _.supportsCorrelatedSubqueries)

  def supportsDataDefinitionAndDataManipulationTransactions: DatabaseMetaData[Boolean] =
    primitive(s"supportsDataDefinitionAndDataManipulationTransactions", _.supportsDataDefinitionAndDataManipulationTransactions)

  def supportsDataManipulationTransactionsOnly: DatabaseMetaData[Boolean] =
    primitive(s"supportsDataManipulationTransactionsOnly", _.supportsDataManipulationTransactionsOnly)

  def supportsDifferentTableCorrelationNames: DatabaseMetaData[Boolean] =
    primitive(s"supportsDifferentTableCorrelationNames", _.supportsDifferentTableCorrelationNames)

  def supportsExpressionsInOrderBy: DatabaseMetaData[Boolean] =
    primitive(s"supportsExpressionsInOrderBy", _.supportsExpressionsInOrderBy)

  def supportsExtendedSQLGrammar: DatabaseMetaData[Boolean] =
    primitive(s"supportsExtendedSQLGrammar", _.supportsExtendedSQLGrammar)

  def supportsFullOuterJoins: DatabaseMetaData[Boolean] =
    primitive(s"supportsFullOuterJoins", _.supportsFullOuterJoins)

  def supportsGetGeneratedKeys: DatabaseMetaData[Boolean] =
    primitive(s"supportsGetGeneratedKeys", _.supportsGetGeneratedKeys)

  def supportsGroupBy: DatabaseMetaData[Boolean] =
    primitive(s"supportsGroupBy", _.supportsGroupBy)

  def supportsGroupByBeyondSelect: DatabaseMetaData[Boolean] =
    primitive(s"supportsGroupByBeyondSelect", _.supportsGroupByBeyondSelect)

  def supportsGroupByUnrelated: DatabaseMetaData[Boolean] =
    primitive(s"supportsGroupByUnrelated", _.supportsGroupByUnrelated)

  def supportsIntegrityEnhancementFacility: DatabaseMetaData[Boolean] =
    primitive(s"supportsIntegrityEnhancementFacility", _.supportsIntegrityEnhancementFacility)

  def supportsLikeEscapeClause: DatabaseMetaData[Boolean] =
    primitive(s"supportsLikeEscapeClause", _.supportsLikeEscapeClause)

  def supportsLimitedOuterJoins: DatabaseMetaData[Boolean] =
    primitive(s"supportsLimitedOuterJoins", _.supportsLimitedOuterJoins)

  def supportsMinimumSQLGrammar: DatabaseMetaData[Boolean] =
    primitive(s"supportsMinimumSQLGrammar", _.supportsMinimumSQLGrammar)

  def supportsMixedCaseIdentifiers: DatabaseMetaData[Boolean] =
    primitive(s"supportsMixedCaseIdentifiers", _.supportsMixedCaseIdentifiers)

  def supportsMixedCaseQuotedIdentifiers: DatabaseMetaData[Boolean] =
    primitive(s"supportsMixedCaseQuotedIdentifiers", _.supportsMixedCaseQuotedIdentifiers)

  def supportsMultipleOpenResults: DatabaseMetaData[Boolean] =
    primitive(s"supportsMultipleOpenResults", _.supportsMultipleOpenResults)

  def supportsMultipleResultSets: DatabaseMetaData[Boolean] =
    primitive(s"supportsMultipleResultSets", _.supportsMultipleResultSets)

  def supportsMultipleTransactions: DatabaseMetaData[Boolean] =
    primitive(s"supportsMultipleTransactions", _.supportsMultipleTransactions)

  def supportsNamedParameters: DatabaseMetaData[Boolean] =
    primitive(s"supportsNamedParameters", _.supportsNamedParameters)

  def supportsNonNullableColumns: DatabaseMetaData[Boolean] =
    primitive(s"supportsNonNullableColumns", _.supportsNonNullableColumns)

  def supportsOpenCursorsAcrossCommit: DatabaseMetaData[Boolean] =
    primitive(s"supportsOpenCursorsAcrossCommit", _.supportsOpenCursorsAcrossCommit)

  def supportsOpenCursorsAcrossRollback: DatabaseMetaData[Boolean] =
    primitive(s"supportsOpenCursorsAcrossRollback", _.supportsOpenCursorsAcrossRollback)

  def supportsOpenStatementsAcrossCommit: DatabaseMetaData[Boolean] =
    primitive(s"supportsOpenStatementsAcrossCommit", _.supportsOpenStatementsAcrossCommit)

  def supportsOpenStatementsAcrossRollback: DatabaseMetaData[Boolean] =
    primitive(s"supportsOpenStatementsAcrossRollback", _.supportsOpenStatementsAcrossRollback)

  def supportsOrderByUnrelated: DatabaseMetaData[Boolean] =
    primitive(s"supportsOrderByUnrelated", _.supportsOrderByUnrelated)

  def supportsOuterJoins: DatabaseMetaData[Boolean] =
    primitive(s"supportsOuterJoins", _.supportsOuterJoins)

  def supportsPositionedDelete: DatabaseMetaData[Boolean] =
    primitive(s"supportsPositionedDelete", _.supportsPositionedDelete)

  def supportsPositionedUpdate: DatabaseMetaData[Boolean] =
    primitive(s"supportsPositionedUpdate", _.supportsPositionedUpdate)

  // todo: enum
  def supportsResultSetConcurrency(kind: Int, concurrency: Int): DatabaseMetaData[Boolean] =
    primitive(s"supportsResultSetConcurrency($kind, $concurrency)", _.supportsResultSetConcurrency(kind, concurrency))

  def supportsResultSetHoldability(holdability: Holdability): DatabaseMetaData[Boolean] =
    primitive(s"supportsResultSetHoldability($holdability)", _.supportsResultSetHoldability(holdability.toInt))

  def supportsResultSetType(kind: ResultSetType): DatabaseMetaData[Boolean] =
    primitive(s"supportsResultSetType($kind)", _.supportsResultSetType(kind.toInt))

  def supportsSavepoInts: DatabaseMetaData[Boolean] =
    primitive(s"supportsSavepoInts", _.supportsSavepoints)

  def supportsSchemasInDataManipulation: DatabaseMetaData[Boolean] =
    primitive(s"supportsSchemasInDataManipulation", _.supportsSchemasInDataManipulation)

  def supportsSchemasInIndexDefinitions: DatabaseMetaData[Boolean] =
    primitive(s"supportsSchemasInIndexDefinitions", _.supportsSchemasInIndexDefinitions)

  def supportsSchemasInPrivilegeDefinitions: DatabaseMetaData[Boolean] =
    primitive(s"supportsSchemasInPrivilegeDefinitions", _.supportsSchemasInPrivilegeDefinitions)

  def supportsSchemasInProcedureCalls: DatabaseMetaData[Boolean] =
    primitive(s"supportsSchemasInProcedureCalls", _.supportsSchemasInProcedureCalls)

  def supportsSchemasInTableDefinitions: DatabaseMetaData[Boolean] =
    primitive(s"supportsSchemasInTableDefinitions", _.supportsSchemasInTableDefinitions)

  def supportsSelectForUpdate: DatabaseMetaData[Boolean] =
    primitive(s"supportsSelectForUpdate", _.supportsSelectForUpdate)

  def supportsStatementPooling: DatabaseMetaData[Boolean] =
    primitive(s"supportsStatementPooling", _.supportsStatementPooling)

  def supportsStoredFunctionsUsingCallSyntax: DatabaseMetaData[Boolean] =
    primitive(s"supportsStoredFunctionsUsingCallSyntax", _.supportsStoredFunctionsUsingCallSyntax)

  def supportsStoredProcedures: DatabaseMetaData[Boolean] =
    primitive(s"supportsStoredProcedures", _.supportsStoredProcedures)

  def supportsSubqueriesInComparisons: DatabaseMetaData[Boolean] =
    primitive(s"supportsSubqueriesInComparisons", _.supportsSubqueriesInComparisons)

  def supportsSubqueriesInExists: DatabaseMetaData[Boolean] =
    primitive(s"supportsSubqueriesInExists", _.supportsSubqueriesInExists)

  def supportsSubqueriesInIns: DatabaseMetaData[Boolean] =
    primitive(s"supportsSubqueriesInIns", _.supportsSubqueriesInIns)

  def supportsSubqueriesInQuantifieds: DatabaseMetaData[Boolean] =
    primitive(s"supportsSubqueriesInQuantifieds", _.supportsSubqueriesInQuantifieds)

  def supportsTableCorrelationNames: DatabaseMetaData[Boolean] =
    primitive(s"supportsTableCorrelationNames", _.supportsTableCorrelationNames)

  def supportsTransactionIsolationLevel(level: TransactionIsolation): DatabaseMetaData[Boolean] =
    primitive(s"supportsTransactionIsolationLevel(level)", _.supportsTransactionIsolationLevel(level.toInt))

  def supportsTransactions: DatabaseMetaData[Boolean] =
    primitive(s"supportsTransactions", _.supportsTransactions)

  def supportsUnion: DatabaseMetaData[Boolean] =
    primitive(s"supportsUnion", _.supportsUnion)

  def supportsUnionAll: DatabaseMetaData[Boolean] =
    primitive(s"supportsUnionAll", _.supportsUnionAll)

  // todo: enum
  def updatesAreDetected(kind: Int): DatabaseMetaData[Boolean] =
    primitive(s"updatesAreDetected($kind)", _.updatesAreDetected(kind))

  def usesLocalFilePerTable: DatabaseMetaData[Boolean] =
    primitive(s"usesLocalFilePerTable", _.usesLocalFilePerTable)

  def usesLocalFiles: DatabaseMetaData[Boolean] =
    primitive(s"usesLocalFiles", _.usesLocalFiles)

}