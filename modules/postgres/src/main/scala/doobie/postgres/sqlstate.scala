// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import doobie.enum.SqlState

/** Module of SQLSTATE constants for PostgreSQL. */
object sqlstate {

  /** Class 00 — Successful Completion */
  object class00 {
    val SUCCESSFUL_COMPLETION = SqlState("00000")
  }

  /** Class 01 — Warning */
  object class01 {
    val WARNING = SqlState("01000")
    val DYNAMIC_RESULT_SETS_RETURNED = SqlState("0100C")
    val IMPLICIT_ZERO_BIT_PADDING = SqlState("01008")
    val NULL_VALUE_ELIMINATED_IN_SET_FUNCTION = SqlState("01003")
    val PRIVILEGE_NOT_GRANTED = SqlState("01007")
    val PRIVILEGE_NOT_REVOKED = SqlState("01006")
    val STRING_DATA_RIGHT_TRUNCATION = SqlState("01004")
    val DEPRECATED_FEATURE = SqlState("01P01")
  }

  /** Class 02 — No Data (this is also a warning class per the SQL standard) */
  object class02 {
    val NO_DATA = SqlState("02000")
    val NO_ADDITIONAL_DYNAMIC_RESULT_SETS_RETURNED = SqlState("02001")
  }

  /** Class 03 — SQL_Statement Not Yet Complete */
  object class03 {
    val SQL_STATEMENT_NOT_YET_COMPLETE = SqlState("03000")
  }

  /** Class 08 — Connection Exception */
  object class08 {
    val CONNECTION_EXCEPTION = SqlState("08000")
    val CONNECTION_DOES_NOT_EXIST = SqlState("08003")
    val CONNECTION_FAILURE = SqlState("08006")
    val SQLCLIENT_UNABLE_TO_ESTABLISH_SQLCONNECTION = SqlState("08001")
    val SQLSERVER_REJECTED_ESTABLISHMENT_OF_SQLCONNECTION = SqlState("08004")
    val TRANSACTION_RESOLUTION_UNKNOWN = SqlState("08007")
    val PROTOCOL_VIOLATION = SqlState("08P01")
  }

  /** Class 09 — Triggered Action Exception */
  object class09 {
    val TRIGGERED_ACTION_EXCEPTION = SqlState("09000")
  }

  /** Class 0A — Feature Not Supported */
  object class0A {
    val FEATURE_NOT_SUPPORTED = SqlState("0A000")
  }

  /** Class 0B — Invalid Transaction Initiation */
  object class0B {
    val INVALID_TRANSACTION_INITIATION = SqlState("0B000")
  }

  /** Class 0F — Locator Exception */
  object class0F {
    val LOCATOR_EXCEPTION = SqlState("0F000")
    val INVALID_LOCATOR_SPECIFICATION = SqlState("0F001")
  }

  /** Class 0L — Invalid Grantor */
  object class0L {
    val INVALID_GRANTOR = SqlState("0L000")
    val INVALID_GRANT_OPERATION = SqlState("0LP01")
  }

  /** Class 0P — Invalid Role Specification */
  object class0P {
    val INVALID_ROLE_SPECIFICATION = SqlState("0P000")
  }

  /** Class 20 — Case Not Found */
  object class20 {
    val CASE_NOT_FOUND = SqlState("20000")
  }

  /** Class 21 — Cardinality Violation */
  object class21 {
    val CARDINALITY_VIOLATION = SqlState("21000")
  }

  /** Class 22 — Data Exception */
  object class22 {
    val DATA_EXCEPTION = SqlState("22000")
    val ARRAY_SUBSCRIPT_ERROR = SqlState("2202E")
    val CHARACTER_NOT_IN_REPERTOIRE = SqlState("22021")
    val DATETIME_FIELD_OVERFLOW = SqlState("22008")
    val DIVISION_BY_ZERO = SqlState("22012")
    val ERROR_IN_ASSIGNMENT = SqlState("22005")
    val ESCAPE_CHARACTER_CONFLICT = SqlState("2200B")
    val INDICATOR_OVERFLOW = SqlState("22022")
    val INTERVAL_FIELD_OVERFLOW = SqlState("22015")
    val INVALID_ARGUMENT_FOR_LOGARITHM = SqlState("2201E")
    val INVALID_ARGUMENT_FOR_NTILE_FUNCTION = SqlState("22014")
    val INVALID_ARGUMENT_FOR_NTH_VALUE_FUNCTION = SqlState("22016")
    val INVALID_ARGUMENT_FOR_POWER_FUNCTION = SqlState("2201F")
    val INVALID_ARGUMENT_FOR_WIDTH_BUCKET_FUNCTION = SqlState("2201G")
    val INVALID_CHARACTER_VALUE_FOR_CAST = SqlState("22018")
    val INVALID_DATETIME_FORMAT = SqlState("22007")
    val INVALID_ESCAPE_CHARACTER = SqlState("22019")
    val INVALID_ESCAPE_OCTET = SqlState("2200D")
    val INVALID_ESCAPE_SEQUENCE = SqlState("22025")
    val NONSTANDARD_USE_OF_ESCAPE_CHARACTER = SqlState("22P06")
    val INVALID_INDICATOR_PARAMETER_VALUE = SqlState("22010")
    val INVALID_PARAMETER_VALUE = SqlState("22023")
    val INVALID_REGULAR_EXPRESSION = SqlState("2201B")
    val INVALID_ROW_COUNT_IN_LIMIT_CLAUSE = SqlState("2201W")
    val INVALID_ROW_COUNT_IN_RESULT_OFFSET_CLAUSE = SqlState("2201X")
    val INVALID_TIME_ZONE_DISPLACEMENT_VALUE = SqlState("22009")
    val INVALID_USE_OF_ESCAPE_CHARACTER = SqlState("2200C")
    val MOST_SPECIFIC_TYPE_MISMATCH = SqlState("2200G")
    val NULL_VALUE_NOT_ALLOWED = SqlState("22004")
    val NULL_VALUE_NO_INDICATOR_PARAMETER = SqlState("22002")
    val NUMERIC_VALUE_OUT_OF_RANGE = SqlState("22003")
    val STRING_DATA_LENGTH_MISMATCH = SqlState("22026")
    val STRING_DATA_RIGHT_TRUNCATION = SqlState("22001")
    val SUBSTRING_ERROR = SqlState("22011")
    val TRIM_ERROR = SqlState("22027")
    val UNTERMINATED_C_STRING = SqlState("22024")
    val ZERO_LENGTH_CHARACTER_STRING = SqlState("2200F")
    val FLOATING_POINT_EXCEPTION = SqlState("22P01")
    val INVALID_TEXT_REPRESENTATION = SqlState("22P02")
    val INVALID_BINARY_REPRESENTATION = SqlState("22P03")
    val BAD_COPY_FILE_FORMAT = SqlState("22P04")
    val UNTRANSLATABLE_CHARACTER = SqlState("22P05")
    val NOT_AN_XML_DOCUMENT = SqlState("2200L")
    val INVALID_XML_DOCUMENT = SqlState("2200M")
    val INVALID_XML_CONTENT = SqlState("2200N")
    val INVALID_XML_COMMENT = SqlState("2200S")
    val INVALID_XML_PROCESSING_INSTRUCTION = SqlState("2200T")
  }

  /** Class 23 — Integrity Constraint Violation */
  object class23 {
    val INTEGRITY_CONSTRAINT_VIOLATION = SqlState("23000")
    val RESTRICT_VIOLATION = SqlState("23001")
    val NOT_NULL_VIOLATION = SqlState("23502")
    val FOREIGN_KEY_VIOLATION = SqlState("23503")
    val UNIQUE_VIOLATION = SqlState("23505")
    val CHECK_VIOLATION = SqlState("23514")
    val EXCLUSION_VIOLATION = SqlState("23P01")
  }

  /** Class 24 — Invalid Cursor State */
  object class24 {
    val INVALID_CURSOR_STATE = SqlState("24000")
  }

  /** Class 25 — Invalid Transaction State */
  object class25 {
    val INVALID_TRANSACTION_STATE = SqlState("25000")
    val ACTIVE_SQL_TRANSACTION = SqlState("25001")
    val BRANCH_TRANSACTION_ALREADY_ACTIVE = SqlState("25002")
    val HELD_CURSOR_REQUIRES_SAME_ISOLATION_LEVEL = SqlState("25008")
    val INAPPROPRIATE_ACCESS_MODE_FOR_BRANCH_TRANSACTION = SqlState("25003")
    val INAPPROPRIATE_ISOLATION_LEVEL_FOR_BRANCH_TRANSACTION = SqlState("25004")
    val NO_ACTIVE_SQL_TRANSACTION_FOR_BRANCH_TRANSACTION = SqlState("25005")
    val READ_ONLY_SQL_TRANSACTION = SqlState("25006")
    val SCHEMA_AND_DATA_STATEMENT_MIXING_NOT_SUPPORTED = SqlState("25007")
    val NO_ACTIVE_SQL_TRANSACTION = SqlState("25P01")
    val IN_FAILED_SQL_TRANSACTION = SqlState("25P02")
  }

  /** Class 26 — Invalid SQL_Statement Name */
  object class26 {
    val INVALID_SQL_STATEMENT_NAME = SqlState("26000")
  }

  /** Class 27 — Triggered Data Change Violation */
  object class27 {
    val TRIGGERED_DATA_CHANGE_VIOLATION = SqlState("27000")
  }

  /** Class 28 — Invalid Authorization Specification */
  object class28 {
    val INVALID_AUTHORIZATION_SPECIFICATION = SqlState("28000")
    val INVALID_PASSWORD = SqlState("28P01")
  }

  /** Class 2B — Dependent Privilege Descriptors Still Exist */
  object class2B {
    val DEPENDENT_PRIVILEGE_DESCRIPTORS_STILL_EXIST = SqlState("2B000")
    val DEPENDENT_OBJECTS_STILL_EXIST = SqlState("2BP01")
  }

  /** Class 2D — Invalid Transaction Termination */
  object class2D {
    val INVALID_TRANSACTION_TERMINATION = SqlState("2D000")
  }

  /** Class 2F — SQL_Routine Exception */
  object class2F {
    val SQL_ROUTINE_EXCEPTION = SqlState("2F000")
    val FUNCTION_EXECUTED_NO_RETURN_STATEMENT = SqlState("2F005")
    val MODIFYING_SQL_DATA_NOT_PERMITTED = SqlState("2F002")
    val PROHIBITED_SQL_STATEMENT_ATTEMPTED = SqlState("2F003")
    val READING_SQL_DATA_NOT_PERMITTED = SqlState("2F004")
  }

  /** Class 34 — Invalid Cursor Name */
  object class34 {
    val INVALID_CURSOR_NAME = SqlState("34000")
  }

  /** Class 38 — External Routine Exception */
  object class38 {
    val EXTERNAL_ROUTINE_EXCEPTION = SqlState("38000")
    val CONTAINING_SQL_NOT_PERMITTED = SqlState("38001")
    val MODIFYING_SQL_DATA_NOT_PERMITTED = SqlState("38002")
    val PROHIBITED_SQL_STATEMENT_ATTEMPTED = SqlState("38003")
    val READING_SQL_DATA_NOT_PERMITTED = SqlState("38004")
  }

  /** Class 39 — External Routine Invocation Exception */
  object class39 {
    val EXTERNAL_ROUTINE_INVOCATION_EXCEPTION = SqlState("39000")
    val INVALID_SQLSTATE_RETURNED = SqlState("39001")
    val NULL_VALUE_NOT_ALLOWED = SqlState("39004")
    val TRIGGER_PROTOCOL_VIOLATED = SqlState("39P01")
    val SRF_PROTOCOL_VIOLATED = SqlState("39P02")
  }

  /** Class 3B — Savepoint Exception */
  object class3B {
    val SAVEPOINT_EXCEPTION = SqlState("3B000")
    val INVALID_SAVEPOINT_SPECIFICATION = SqlState("3B001")
  }

  /** Class 3D — Invalid Catalog Name */
  object class3D {
    val INVALID_CATALOG_NAME = SqlState("3D000")
  }

  /** Class 3F — Invalid Schema Name */
  object class3F {
    val INVALID_SCHEMA_NAME = SqlState("3F000")
  }

  /** Class 40 — Transaction Rollback */
  object class40 {
    val TRANSACTION_ROLLBACK = SqlState("40000")
    val TRANSACTION_INTEGRITY_CONSTRAINT_VIOLATION = SqlState("40002")
    val SERIALIZATION_FAILURE = SqlState("40001")
    val STATEMENT_COMPLETION_UNKNOWN = SqlState("40003")
    val DEADLOCK_DETECTED = SqlState("40P01")
  }

  /** Class 42 — Syntax Error or Access Rule Violation */
  object class42 {
    val SYNTAX_ERROR_OR_ACCESS_RULE_VIOLATION = SqlState("42000")
    val SYNTAX_ERROR = SqlState("42601")
    val INSUFFICIENT_PRIVILEGE = SqlState("42501")
    val CANNOT_COERCE = SqlState("42846")
    val GROUPING_ERROR = SqlState("42803")
    val WINDOWING_ERROR = SqlState("42P20")
    val INVALID_RECURSION = SqlState("42P19")
    val INVALID_FOREIGN_KEY = SqlState("42830")
    val INVALID_NAME = SqlState("42602")
    val NAME_TOO_LONG = SqlState("42622")
    val RESERVED_NAME = SqlState("42939")
    val DATATYPE_MISMATCH = SqlState("42804")
    val INDETERMINATE_DATATYPE = SqlState("42P18")
    val WRONG_OBJECT_TYPE = SqlState("42809")
    val UNDEFINED_COLUMN = SqlState("42703")
    val UNDEFINED_FUNCTION = SqlState("42883")
    val UNDEFINED_TABLE = SqlState("42P01")
    val UNDEFINED_PARAMETER = SqlState("42P02")
    val UNDEFINED_OBJECT = SqlState("42704")
    val DUPLICATE_COLUMN = SqlState("42701")
    val DUPLICATE_CURSOR = SqlState("42P03")
    val DUPLICATE_DATABASE = SqlState("42P04")
    val DUPLICATE_FUNCTION = SqlState("42723")
    val DUPLICATE_PREPARED_STATEMENT = SqlState("42P05")
    val DUPLICATE_SCHEMA = SqlState("42P06")
    val DUPLICATE_TABLE = SqlState("42P07")
    val DUPLICATE_ALIAS = SqlState("42712")
    val DUPLICATE_OBJECT = SqlState("42710")
    val AMBIGUOUS_COLUMN = SqlState("42702")
    val AMBIGUOUS_FUNCTION = SqlState("42725")
    val AMBIGUOUS_PARAMETER = SqlState("42P08")
    val AMBIGUOUS_ALIAS = SqlState("42P09")
    val INVALID_COLUMN_REFERENCE = SqlState("42P10")
    val INVALID_COLUMN_DEFINITION = SqlState("42611")
    val INVALID_CURSOR_DEFINITION = SqlState("42P11")
    val INVALID_DATABASE_DEFINITION = SqlState("42P12")
    val INVALID_FUNCTION_DEFINITION = SqlState("42P13")
    val INVALID_PREPARED_STATEMENT_DEFINITION = SqlState("42P14")
    val INVALID_SCHEMA_DEFINITION = SqlState("42P15")
    val INVALID_TABLE_DEFINITION = SqlState("42P16")
    val INVALID_OBJECT_DEFINITION = SqlState("42P17")
  }

  /** Class 44 — WITH_CHECK_OPTION_Violation */
  object class44 {
    val WITH_CHECK_OPTION_VIOLATION = SqlState("44000")
  }

  /** Class 53 — Insufficient Resources */
  object class53 {
    val INSUFFICIENT_RESOURCES = SqlState("53000")
    val DISK_FULL = SqlState("53100")
    val OUT_OF_MEMORY = SqlState("53200")
    val TOO_MANY_CONNECTIONS = SqlState("53300")
  }

  /** Class 54 — Program Limit Exceeded */
  object class54 {
    val PROGRAM_LIMIT_EXCEEDED = SqlState("54000")
    val STATEMENT_TOO_COMPLEX = SqlState("54001")
    val TOO_MANY_COLUMNS = SqlState("54011")
    val TOO_MANY_ARGUMENTS = SqlState("54023")
  }

  /** Class 55 — Object Not In Prerequisite State */
  object class55 {
    val OBJECT_NOT_IN_PREREQUISITE_STATE = SqlState("55000")
    val OBJECT_IN_USE = SqlState("55006")
    val CANT_CHANGE_RUNTIME_PARAM = SqlState("55P02")
    val LOCK_NOT_AVAILABLE = SqlState("55P03")
  }

  /** Class 57 — Operator Intervention */
  object class57 {
    val OPERATOR_INTERVENTION = SqlState("57000")
    val QUERY_CANCELED = SqlState("57014")
    val ADMIN_SHUTDOWN = SqlState("57P01")
    val CRASH_SHUTDOWN = SqlState("57P02")
    val CANNOT_CONNECT_NOW = SqlState("57P03")
    val DATABASE_DROPPED = SqlState("57P04")
  }

  /** Class 58 — System Error (errors external to PostgreSQL itself) */
  object class58 {
    val IO_ERROR = SqlState("58030")
    val UNDEFINED_FILE = SqlState("58P01")
    val DUPLICATE_FILE = SqlState("58P02")
  }

  /** Class F0 — Configuration File Error */
  object classF0 {
    val CONFIG_FILE_ERROR = SqlState("F0000")
    val LOCK_FILE_EXISTS = SqlState("F0001")
  }

  /** Class P0 — PL/pgSQL_Error */
  object classP0 {
    val PLPGSQL_ERROR = SqlState("P0000")
    val RAISE_EXCEPTION = SqlState("P0001")
    val NO_DATA_FOUND = SqlState("P0002")
    val TOO_MANY_ROWS = SqlState("P0003")
  }

  /** Class XX — Internal Error */
  object classXX {
    val INTERNAL_ERROR = SqlState("XX000")
    val DATA_CORRUPTED = SqlState("XX001")
    val INDEX_CORRUPTED = SqlState("XX002")
  }

}
