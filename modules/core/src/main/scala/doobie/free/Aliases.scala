package doobie.free

trait Types {
  type BlobIO[A]              = blob.BlobIO[A]
  type CallableStatementIO[A] = callablestatement.CallableStatementIO[A]
  type ClobIO[A]              = clob.ClobIO[A]
  type ConnectionIO[A]        = connection.ConnectionIO[A]
  type DatabaseMetaDataIO[A]  = databasemetadata.DatabaseMetaDataIO[A]
  type DriverIO[A]            = driver.DriverIO[A]
  type NClobIO[A]             = nclob.NClobIO[A]
  type PreparedStatementIO[A] = preparedstatement.PreparedStatementIO[A]
  type RefIO[A]               = ref.RefIO[A]
  type ResultSetIO[A]         = resultset.ResultSetIO[A]
  type SQLDataIO[A]           = sqldata.SQLDataIO[A]
  type SQLInputIO[A]          = sqlinput.SQLInputIO[A]
  type SQLOutputIO[A]         = sqloutput.SQLOutputIO[A]
  type StatementIO[A]         = statement.StatementIO[A]
}

trait Modules {
  val FB   = blob
  val FCS  = callablestatement
  val FCL  = clob
  val FC   = connection
  val FDMD = databasemetadata
  val FD   = driver
  val FNCL = nclob
  val FPS  = preparedstatement
  val FREF = ref
  val FRS  = resultset
  val FSD  = sqldata
  val FSI  = sqlinput
  val FSO  = sqloutput
  val FS   = statement
}

trait Instances {
  implicit val AsyncBlobIO              = blob.AsyncBlobIO
  implicit val AsyncCallableStatementIO = callablestatement.AsyncCallableStatementIO
  implicit val AsyncClobIO              = clob.AsyncClobIO
  implicit val AsyncConnectionIO        = connection.AsyncConnectionIO
  implicit val AsyncDatabaseMetaDataIO  = databasemetadata.AsyncDatabaseMetaDataIO
  implicit val AsyncDriverIO            = driver.AsyncDriverIO
  implicit val AsyncNClobIO             = nclob.AsyncNClobIO
  implicit val AsyncPreparedStatementIO = preparedstatement.AsyncPreparedStatementIO
  implicit val AsyncRefIO               = ref.AsyncRefIO
  implicit val AsyncResultSetIO         = resultset.AsyncResultSetIO
  implicit val AsyncSQLDataIO           = sqldata.AsyncSQLDataIO
  implicit val AsyncSQLInputIO          = sqlinput.AsyncSQLInputIO
  implicit val AsyncSQLOutputIO         = sqloutput.AsyncSQLOutputIO
  implicit val AsyncStatementIO         = statement.AsyncStatementIO
}
