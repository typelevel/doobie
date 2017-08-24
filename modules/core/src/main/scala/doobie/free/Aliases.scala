// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

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
  lazy val FB   = blob
  lazy val FCS  = callablestatement
  lazy val FCL  = clob
  lazy val FC   = connection
  lazy val FDMD = databasemetadata
  lazy val FD   = driver
  lazy val FNCL = nclob
  lazy val FPS  = preparedstatement
  lazy val FREF = ref
  lazy val FRS  = resultset
  lazy val FSD  = sqldata
  lazy val FSI  = sqlinput
  lazy val FSO  = sqloutput
  lazy val FS   = statement
}

trait Instances {
  implicit lazy val AsyncBlobIO              = blob.AsyncBlobIO
  implicit lazy val AsyncCallableStatementIO = callablestatement.AsyncCallableStatementIO
  implicit lazy val AsyncClobIO              = clob.AsyncClobIO
  implicit lazy val AsyncConnectionIO        = connection.AsyncConnectionIO
  implicit lazy val AsyncDatabaseMetaDataIO  = databasemetadata.AsyncDatabaseMetaDataIO
  implicit lazy val AsyncDriverIO            = driver.AsyncDriverIO
  implicit lazy val AsyncNClobIO             = nclob.AsyncNClobIO
  implicit lazy val AsyncPreparedStatementIO = preparedstatement.AsyncPreparedStatementIO
  implicit lazy val AsyncRefIO               = ref.AsyncRefIO
  implicit lazy val AsyncResultSetIO         = resultset.AsyncResultSetIO
  implicit lazy val AsyncSQLDataIO           = sqldata.AsyncSQLDataIO
  implicit lazy val AsyncSQLInputIO          = sqlinput.AsyncSQLInputIO
  implicit lazy val AsyncSQLOutputIO         = sqloutput.AsyncSQLOutputIO
  implicit lazy val AsyncStatementIO         = statement.AsyncStatementIO
}
