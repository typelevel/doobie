package doobie.free

trait Aliases {

  val FB = blob
  implicit val AsyncBlobIO = FB.AsyncBlobIO
  type BlobIO[A] = FB.BlobIO[A]

  val FCS = callablestatement
  implicit val AsyncCallableStatementIO = FCS.AsyncCallableStatementIO
  type CallableStatementIO[A] = FCS.CallableStatementIO[A]

  val FCL = clob
  implicit val AsyncClobIO = FCL.AsyncClobIO
  type ClobIO[A] = FCL.ClobIO[A]

  val FC = connection
  implicit val AsyncConnectionIO = FC.AsyncConnectionIO
  type ConnectionIO[A] = FC.ConnectionIO[A]

  val FDMD = databasemetadata
  implicit val AsyncDatabaseMetaDataIO = FDMD.AsyncDatabaseMetaDataIO
  type DatabaseMetaDataIO[A] = FDMD.DatabaseMetaDataIO[A]

  val FD = driver
  implicit val AsyncDriverIO = FD.AsyncDriverIO
  type DriverIO[A] = FD.DriverIO[A]

  val FNCL = nclob
  implicit val AsyncNClobIO = FNCL.AsyncNClobIO
  type NClobIO[A] = FNCL.NClobIO[A]

  val FPS = preparedstatement
  implicit val AsyncPreparedStatementIO = FPS.AsyncPreparedStatementIO
  type PreparedStatementIO[A] = FPS.PreparedStatementIO[A]

  val FREF = ref
  implicit val AsyncRefIO = FREF.AsyncRefIO
  type RefIO[A] = FREF.RefIO[A]

  val FRS = resultset
  implicit val AsyncResultSetIO = FRS.AsyncResultSetIO
  type ResultSetIO[A] = FRS.ResultSetIO[A]

  val FSD = sqldata
  implicit val AsyncSQLDataIO = FSD.AsyncSQLDataIO
  type SQLDataIO[A] = FSD.SQLDataIO[A]

  val FSI = sqlinput
  implicit val AsyncSQLInputIO = FSI.AsyncSQLInputIO
  type SQLInputIO[A] = FSI.SQLInputIO[A]

  val FSO = sqloutput
  implicit val AsyncSQLOutputIO = FSO.AsyncSQLOutputIO
  type SQLOutputIO[A] = FSO.SQLOutputIO[A]

  val FS = statement
  implicit val AsyncStatementIO = FS.AsyncStatementIO
  type StatementIO[A] = FS.StatementIO[A]

}
object Aliases extends Aliases
