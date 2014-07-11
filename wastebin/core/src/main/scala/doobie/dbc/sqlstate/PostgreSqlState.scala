package doobie.dbc.sqlstate

trait PostgreSqlState {

  // Class 00 — Successful Completion
  object class00 {
    val SUCCESSFUL_COMPLETION = "00000"
  }

  // Class 01 — Warning
  object class01 {
    val WARNING = "01000"
    val DYNAMIC_RESULT_SETS_RETURNED = "0100C"
    val IMPLICIT_ZERO_BIT_PADDING = "01008"
    val NULL_VALUE_ELIMINATED_IN_SET_FUNCTION = "01003"
    val PRIVILEGE_NOT_GRANTED = "01007"
    val PRIVILEGE_NOT_REVOKED = "01006"
    val STRING_DATA_RIGHT_TRUNCATION = "01004"
    val DEPRECATED_FEATURE = "01P01"
  }

  // Class 02 — No Data (this is also a warning class per the SQL standard)
  object class02 {
    val NO_DATA = "02000"
    val NO_ADDITIONAL_DYNAMIC_RESULT_SETS_RETURNED = "02001"
  }

  // Class 03 — SQL_Statement Not Yet Complete
  object class03 {
    val SQL_STATEMENT_NOT_YET_COMPLETE = "03000"
  }

  // Class 08 — Connection Exception
  object class08 {
    val CONNECTION_EXCEPTION = "08000"
    val CONNECTION_DOES_NOT_EXIST = "08003"
    val CONNECTION_FAILURE = "08006"
    val SQLCLIENT_UNABLE_TO_ESTABLISH_SQLCONNECTION = "08001"
    val SQLSERVER_REJECTED_ESTABLISHMENT_OF_SQLCONNECTION = "08004"
    val TRANSACTION_RESOLUTION_UNKNOWN = "08007"
    val PROTOCOL_VIOLATION = "08P01"
  }

  // Class 09 — Triggered Action Exception
  object class09 {
    val TRIGGERED_ACTION_EXCEPTION = "09000"
  }

  // Class 0A — Feature Not Supported
  object class0A {
    val FEATURE_NOT_SUPPORTED = "0A000"
  }

  // Class 0B — Invalid Transaction Initiation
  object class0B {
    val INVALID_TRANSACTION_INITIATION = "0B000"
  }

  // Class 0F — Locator Exception
  object class0F {
    val LOCATOR_EXCEPTION = "0F000"
    val INVALID_LOCATOR_SPECIFICATION = "0F001"
  }

  // Class 0L — Invalid Grantor
  object class0L {
    val INVALID_GRANTOR = "0L000"
    val INVALID_GRANT_OPERATION = "0LP01"
  }

  // Class 0P — Invalid Role Specification
  object class0P {
    val INVALID_ROLE_SPECIFICATION = "0P000"
  }

  // Class 20 — Case Not Found
  object class20 {
    val CASE_NOT_FOUND = "20000"
  }

  // Class 21 — Cardinality Violation
  object class21 {
    val CARDINALITY_VIOLATION = "21000"
  }

  // Class 22 — Data Exception
  object class22 {
    val DATA_EXCEPTION = "22000"
    val ARRAY_SUBSCRIPT_ERROR = "2202E"
    val CHARACTER_NOT_IN_REPERTOIRE = "22021"
    val DATETIME_FIELD_OVERFLOW = "22008"
    val DIVISION_BY_ZERO = "22012"
    val ERROR_IN_ASSIGNMENT = "22005"
    val ESCAPE_CHARACTER_CONFLICT = "2200B"
    val INDICATOR_OVERFLOW = "22022"
    val INTERVAL_FIELD_OVERFLOW = "22015"
    val INVALID_ARGUMENT_FOR_LOGARITHM = "2201E"
    val INVALID_ARGUMENT_FOR_NTILE_FUNCTION = "22014"
    val INVALID_ARGUMENT_FOR_NTH_VALUE_FUNCTION = "22016"
    val INVALID_ARGUMENT_FOR_POWER_FUNCTION = "2201F"
    val INVALID_ARGUMENT_FOR_WIDTH_BUCKET_FUNCTION = "2201G"
    val INVALID_CHARACTER_VALUE_FOR_CAST = "22018"
    val INVALID_DATETIME_FORMAT = "22007"
    val INVALID_ESCAPE_CHARACTER = "22019"
    val INVALID_ESCAPE_OCTET = "2200D"
    val INVALID_ESCAPE_SEQUENCE = "22025"
    val NONSTANDARD_USE_OF_ESCAPE_CHARACTER = "22P06"
    val INVALID_INDICATOR_PARAMETER_VALUE = "22010"
    val INVALID_PARAMETER_VALUE = "22023"
    val INVALID_REGULAR_EXPRESSION = "2201B"
    val INVALID_ROW_COUNT_IN_LIMIT_CLAUSE = "2201W"
    val INVALID_ROW_COUNT_IN_RESULT_OFFSET_CLAUSE = "2201X"
    val INVALID_TIME_ZONE_DISPLACEMENT_VALUE = "22009"
    val INVALID_USE_OF_ESCAPE_CHARACTER = "2200C"
    val MOST_SPECIFIC_TYPE_MISMATCH = "2200G"
    val NULL_VALUE_NOT_ALLOWED = "22004"
    val NULL_VALUE_NO_INDICATOR_PARAMETER = "22002"
    val NUMERIC_VALUE_OUT_OF_RANGE = "22003"
    val STRING_DATA_LENGTH_MISMATCH = "22026"
    val STRING_DATA_RIGHT_TRUNCATION = "22001"
    val SUBSTRING_ERROR = "22011"
    val TRIM_ERROR = "22027"
    val UNTERMINATED_C_STRING = "22024"
    val ZERO_LENGTH_CHARACTER_STRING = "2200F"
    val FLOATING_POINT_EXCEPTION = "22P01"
    val INVALID_TEXT_REPRESENTATION = "22P02"
    val INVALID_BINARY_REPRESENTATION = "22P03"
    val BAD_COPY_FILE_FORMAT = "22P04"
    val UNTRANSLATABLE_CHARACTER = "22P05"
    val NOT_AN_XML_DOCUMENT = "2200L"
    val INVALID_XML_DOCUMENT = "2200M"
    val INVALID_XML_CONTENT = "2200N"
    val INVALID_XML_COMMENT = "2200S"
    val INVALID_XML_PROCESSING_INSTRUCTION = "2200T"
  }

  // Class 23 — Integrity Constraint Violation
  object class23 {
    val INTEGRITY_CONSTRAINT_VIOLATION = "23000"
    val RESTRICT_VIOLATION = "23001"
    val NOT_NULL_VIOLATION = "23502"
    val FOREIGN_KEY_VIOLATION = "23503"
    val UNIQUE_VIOLATION = "23505"
    val CHECK_VIOLATION = "23514"
    val EXCLUSION_VIOLATION = "23P01"
  }

  // Class 24 — Invalid Cursor State
  object class24 {
    val INVALID_CURSOR_STATE = "24000"
  }

  // Class 25 — Invalid Transaction State
  object class25 {
    val INVALID_TRANSACTION_STATE = "25000"
    val ACTIVE_SQL_TRANSACTION = "25001"
    val BRANCH_TRANSACTION_ALREADY_ACTIVE = "25002"
    val HELD_CURSOR_REQUIRES_SAME_ISOLATION_LEVEL = "25008"
    val INAPPROPRIATE_ACCESS_MODE_FOR_BRANCH_TRANSACTION = "25003"
    val INAPPROPRIATE_ISOLATION_LEVEL_FOR_BRANCH_TRANSACTION = "25004"
    val NO_ACTIVE_SQL_TRANSACTION_FOR_BRANCH_TRANSACTION = "25005"
    val READ_ONLY_SQL_TRANSACTION = "25006"
    val SCHEMA_AND_DATA_STATEMENT_MIXING_NOT_SUPPORTED = "25007"
    val NO_ACTIVE_SQL_TRANSACTION = "25P01"
    val IN_FAILED_SQL_TRANSACTION = "25P02"
  }

  // Class 26 — Invalid SQL_Statement Name
  object class26 {
    val INVALID_SQL_STATEMENT_NAME = "26000"
  }

  // Class 27 — Triggered Data Change Violation
  object class27 {
    val TRIGGERED_DATA_CHANGE_VIOLATION = "27000"
  }

  // Class 28 — Invalid Authorization Specification
  object class28 {
    val INVALID_AUTHORIZATION_SPECIFICATION = "28000"
    val INVALID_PASSWORD = "28P01"
  }

  // Class 2B — Dependent Privilege Descriptors Still Exist
  object class2B {
    val DEPENDENT_PRIVILEGE_DESCRIPTORS_STILL_EXIST = "2B000"
    val DEPENDENT_OBJECTS_STILL_EXIST = "2BP01"
  }

  // Class 2D — Invalid Transaction Termination
  object class2D {
    val INVALID_TRANSACTION_TERMINATION = "2D000"
  }

  // Class 2F — SQL_Routine Exception
  object class2F {
    val SQL_ROUTINE_EXCEPTION = "2F000"
    val FUNCTION_EXECUTED_NO_RETURN_STATEMENT = "2F005"
    val MODIFYING_SQL_DATA_NOT_PERMITTED = "2F002"
    val PROHIBITED_SQL_STATEMENT_ATTEMPTED = "2F003"
    val READING_SQL_DATA_NOT_PERMITTED = "2F004"
  }

  // Class 34 — Invalid Cursor Name
  object class34 {
    val INVALID_CURSOR_NAME = "34000"
  }

  // Class 38 — External Routine Exception
  object class38 { 
    val EXTERNAL_ROUTINE_EXCEPTION = "38000"
    val CONTAINING_SQL_NOT_PERMITTED = "38001"
    val MODIFYING_SQL_DATA_NOT_PERMITTED = "38002"
    val PROHIBITED_SQL_STATEMENT_ATTEMPTED = "38003"
    val READING_SQL_DATA_NOT_PERMITTED = "38004"
  }

  // Class 39 — External Routine Invocation Exception
  object class39 {
    val EXTERNAL_ROUTINE_INVOCATION_EXCEPTION = "39000"
    val INVALID_SQLSTATE_RETURNED = "39001"
    val NULL_VALUE_NOT_ALLOWED = "39004"
    val TRIGGER_PROTOCOL_VIOLATED = "39P01"
    val SRF_PROTOCOL_VIOLATED = "39P02"
  }

  // Class 3B — Savepoint Exception
  object class3B {
    val SAVEPOINT_EXCEPTION = "3B000"
    val INVALID_SAVEPOINT_SPECIFICATION = "3B001"
  }

  // Class 3D — Invalid Catalog Name
  object class3D {
    val INVALID_CATALOG_NAME = "3D000"
  }

  // Class 3F — Invalid Schema Name
  object class3F {
    val INVALID_SCHEMA_NAME = "3F000"
  }

  // Class 40 — Transaction Rollback
  object class40 {
    val TRANSACTION_ROLLBACK = "40000"
    val TRANSACTION_INTEGRITY_CONSTRAINT_VIOLATION = "40002"
    val SERIALIZATION_FAILURE = "40001"
    val STATEMENT_COMPLETION_UNKNOWN = "40003"
    val DEADLOCK_DETECTED = "40P01"
  }

  // Class 42 — Syntax Error or Access Rule Violation
  object class42 {
    val SYNTAX_ERROR_OR_ACCESS_RULE_VIOLATION = "42000"
    val SYNTAX_ERROR = "42601"
    val INSUFFICIENT_PRIVILEGE = "42501"
    val CANNOT_COERCE = "42846"
    val GROUPING_ERROR = "42803"
    val WINDOWING_ERROR = "42P20"
    val INVALID_RECURSION = "42P19"
    val INVALID_FOREIGN_KEY = "42830"
    val INVALID_NAME = "42602"
    val NAME_TOO_LONG = "42622"
    val RESERVED_NAME = "42939"
    val DATATYPE_MISMATCH = "42804"
    val INDETERMINATE_DATATYPE = "42P18"
    val WRONG_OBJECT_TYPE = "42809"
    val UNDEFINED_COLUMN = "42703"
    val UNDEFINED_FUNCTION = "42883"
    val UNDEFINED_TABLE = "42P01"
    val UNDEFINED_PARAMETER = "42P02"
    val UNDEFINED_OBJECT = "42704"
    val DUPLICATE_COLUMN = "42701"
    val DUPLICATE_CURSOR = "42P03"
    val DUPLICATE_DATABASE = "42P04"
    val DUPLICATE_FUNCTION = "42723"
    val DUPLICATE_PREPARED_STATEMENT = "42P05"
    val DUPLICATE_SCHEMA = "42P06"
    val DUPLICATE_TABLE = "42P07"
    val DUPLICATE_ALIAS = "42712"
    val DUPLICATE_OBJECT = "42710"
    val AMBIGUOUS_COLUMN = "42702"
    val AMBIGUOUS_FUNCTION = "42725"
    val AMBIGUOUS_PARAMETER = "42P08"
    val AMBIGUOUS_ALIAS = "42P09"
    val INVALID_COLUMN_REFERENCE = "42P10"
    val INVALID_COLUMN_DEFINITION = "42611"
    val INVALID_CURSOR_DEFINITION = "42P11"
    val INVALID_DATABASE_DEFINITION = "42P12"
    val INVALID_FUNCTION_DEFINITION = "42P13"
    val INVALID_PREPARED_STATEMENT_DEFINITION = "42P14"
    val INVALID_SCHEMA_DEFINITION = "42P15"
    val INVALID_TABLE_DEFINITION = "42P16"
    val INVALID_OBJECT_DEFINITION = "42P17"
  }

  // Class 44 — WITH_CHECK_OPTION_Violation
  object class44 {
    val WITH_CHECK_OPTION_VIOLATION = "44000"
  }

  // Class 53 — Insufficient Resources
  object class53 {
    val INSUFFICIENT_RESOURCES = "53000"
    val DISK_FULL = "53100"
    val OUT_OF_MEMORY = "53200"
    val TOO_MANY_CONNECTIONS = "53300"
  }

  // Class 54 — Program Limit Exceeded
  object class54 {
    val PROGRAM_LIMIT_EXCEEDED = "54000"
    val STATEMENT_TOO_COMPLEX = "54001"
    val TOO_MANY_COLUMNS = "54011"
    val TOO_MANY_ARGUMENTS = "54023"
  }

  // Class 55 — Object Not In Prerequisite State
  object class55 {
    val OBJECT_NOT_IN_PREREQUISITE_STATE = "55000"
    val OBJECT_IN_USE = "55006"
    val CANT_CHANGE_RUNTIME_PARAM = "55P02"
    val LOCK_NOT_AVAILABLE = "55P03"
  }

  // Class 57 — Operator Intervention
  object class57 {
    val OPERATOR_INTERVENTION = "57000"
    val QUERY_CANCELED = "57014"
    val ADMIN_SHUTDOWN = "57P01"
    val CRASH_SHUTDOWN = "57P02"
    val CANNOT_CONNECT_NOW = "57P03"
    val DATABASE_DROPPED = "57P04"
  }

  // Class 58 — System Error (errors external to PostgreSQL itself)
  object class58 {
    val IO_ERROR = "58030"
    val UNDEFINED_FILE = "58P01"
    val DUPLICATE_FILE = "58P02"
  }

  // Class F0 — Configuration File Error
  object classF0 {
    val CONFIG_FILE_ERROR = "F0000"
    val LOCK_FILE_EXISTS = "F0001"
  }

  // Class P0 — PL/pgSQL_Error
  object classP0 {
    val PLPGSQL_ERROR = "P0000"
    val RAISE_EXCEPTION = "P0001"
    val NO_DATA_FOUND = "P0002"
    val TOO_MANY_ROWS = "P0003"
  }

  // Class XX — Internal Error
  object classXX {
    val INTERNAL_ERROR = "XX000"
    val DATA_CORRUPTED = "XX001"
    val INDEX_CORRUPTED = "XX002"
  }
  
}