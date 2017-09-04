// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.free

import cats.effect.Async

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

  implicit lazy val AsyncBlobIO: Async[BlobIO] =
    blob.AsyncBlobIO

  implicit lazy val AsyncCallableStatementIO: Async[CallableStatementIO] =
    callablestatement.AsyncCallableStatementIO

  implicit lazy val AsyncClobIO: Async[ClobIO] =
    clob.AsyncClobIO

  implicit lazy val AsyncConnectionIO: Async[ConnectionIO] =
    connection.AsyncConnectionIO

  implicit lazy val AsyncDatabaseMetaDataIO: Async[DatabaseMetaDataIO] =
    databasemetadata.AsyncDatabaseMetaDataIO
  implicit lazy val AsyncDriverIO: Async[DriverIO] =
    driver.AsyncDriverIO

  implicit lazy val AsyncNClobIO: Async[NClobIO] =
    nclob.AsyncNClobIO

  implicit lazy val AsyncPreparedStatementIO: Async[PreparedStatementIO] =
    preparedstatement.AsyncPreparedStatementIO

  implicit lazy val AsyncRefIO: Async[RefIO] =
    ref.AsyncRefIO

  implicit lazy val AsyncResultSetIO: Async[ResultSetIO] =
    resultset.AsyncResultSetIO

  implicit lazy val AsyncSQLDataIO: Async[SQLDataIO] =
    sqldata.AsyncSQLDataIO

  implicit lazy val AsyncSQLInputIO: Async[SQLInputIO] =
    sqlinput.AsyncSQLInputIO

  implicit lazy val AsyncSQLOutputIO: Async[SQLOutputIO] =
    sqloutput.AsyncSQLOutputIO

  implicit lazy val AsyncStatementIO: Async[StatementIO] =
    statement.AsyncStatementIO

}
