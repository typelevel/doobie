// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.free

import cats.effect.Async
import cats.effect._
import io.chrisdavenport.log4cats.Logger

trait Types {
  /** @group Type Aliases - Free API */ type BlobIO[A]              = blob.BlobIO[A]
  /** @group Type Aliases - Free API */ type CallableStatementIO[A] = callablestatement.CallableStatementIO[A]
  /** @group Type Aliases - Free API */ type ClobIO[A]              = clob.ClobIO[A]
  /** @group Type Aliases - Free API */ type ConnectionIO[A]        = connection.ConnectionIO[A]
  /** @group Type Aliases - Free API */ type DatabaseMetaDataIO[A]  = databasemetadata.DatabaseMetaDataIO[A]
  /** @group Type Aliases - Free API */ type DriverIO[A]            = driver.DriverIO[A]
  /** @group Type Aliases - Free API */ type NClobIO[A]             = nclob.NClobIO[A]
  /** @group Type Aliases - Free API */ type PreparedStatementIO[A] = preparedstatement.PreparedStatementIO[A]
  /** @group Type Aliases - Free API */ type RefIO[A]               = ref.RefIO[A]
  /** @group Type Aliases - Free API */ type ResultSetIO[A]         = resultset.ResultSetIO[A]
  /** @group Type Aliases - Free API */ type SQLDataIO[A]           = sqldata.SQLDataIO[A]
  /** @group Type Aliases - Free API */ type SQLInputIO[A]          = sqlinput.SQLInputIO[A]
  /** @group Type Aliases - Free API */ type SQLOutputIO[A]         = sqloutput.SQLOutputIO[A]
  /** @group Type Aliases - Free API */ type StatementIO[A]         = statement.StatementIO[A]
}

trait Modules {
  /** @group Module Aliases - Free API */ lazy val FB   = blob
  /** @group Module Aliases - Free API */ lazy val FCS  = callablestatement
  /** @group Module Aliases - Free API */ lazy val FCL  = clob
  /** @group Module Aliases - Free API */ lazy val FC   = connection
  /** @group Module Aliases - Free API */ lazy val FDMD = databasemetadata
  /** @group Module Aliases - Free API */ lazy val FD   = driver
  /** @group Module Aliases - Free API */ lazy val FNCL = nclob
  /** @group Module Aliases - Free API */ lazy val FPS  = preparedstatement
  /** @group Module Aliases - Free API */ lazy val FREF = ref
  /** @group Module Aliases - Free API */ lazy val FRS  = resultset
  /** @group Module Aliases - Free API */ lazy val FSD  = sqldata
  /** @group Module Aliases - Free API */ lazy val FSI  = sqlinput
  /** @group Module Aliases - Free API */ lazy val FSO  = sqloutput
  /** @group Module Aliases - Free API */ lazy val FS   = statement
}

trait Instances {

  /** @group Typeclass Instances */  implicit lazy val AsyncBlobIO: Async[BlobIO] =
    blob.AsyncBlobIO

  /** @group Typeclass Instances */  implicit lazy val AsyncCallableStatementIO: Async[CallableStatementIO] =
    callablestatement.AsyncCallableStatementIO

  /** @group Typeclass Instances */  implicit lazy val AsyncClobIO: Async[ClobIO] =
    clob.AsyncClobIO

  /** @group Typeclass Instances */  implicit lazy val AsyncConnectionIO: Async[ConnectionIO] =
    connection.AsyncConnectionIO

  /** @group Typeclass Instances */  implicit lazy val AsyncDatabaseMetaDataIO: Async[DatabaseMetaDataIO] =
    databasemetadata.AsyncDatabaseMetaDataIO
  /** @group Typeclass Instances */  implicit lazy val AsyncDriverIO: Async[DriverIO] =
    driver.AsyncDriverIO

  /** @group Typeclass Instances */  implicit lazy val AsyncNClobIO: Async[NClobIO] =
    nclob.AsyncNClobIO

  /** @group Typeclass Instances */  implicit lazy val AsyncPreparedStatementIO: Async[PreparedStatementIO] =
    preparedstatement.AsyncPreparedStatementIO

  /** @group Typeclass Instances */  implicit lazy val AsyncRefIO: Async[RefIO] =
    ref.AsyncRefIO

  /** @group Typeclass Instances */  implicit lazy val AsyncResultSetIO: Async[ResultSetIO] =
    resultset.AsyncResultSetIO

  /** @group Typeclass Instances */  implicit lazy val AsyncSQLDataIO: Async[SQLDataIO] =
    sqldata.AsyncSQLDataIO

  /** @group Typeclass Instances */  implicit lazy val AsyncSQLInputIO: Async[SQLInputIO] =
    sqlinput.AsyncSQLInputIO

  /** @group Typeclass Instances */  implicit lazy val AsyncSQLOutputIO: Async[SQLOutputIO] =
    sqloutput.AsyncSQLOutputIO

  /** @group Typeclass Instances */  implicit lazy val AsyncStatementIO: Async[StatementIO] =
    statement.AsyncStatementIO

  /** @group Typeclass Instances */  implicit val ContextShiftBlobIO: ContextShift[BlobIO] =
    blob.ContextShiftBlobIO

  /** @group Typeclass Instances */  implicit val ContextShiftCallableStatementIO: ContextShift[CallableStatementIO] =
    callablestatement.ContextShiftCallableStatementIO

  /** @group Typeclass Instances */  implicit val ContextShiftClobIO: ContextShift[ClobIO] =
    clob.ContextShiftClobIO

  /** @group Typeclass Instances */  implicit val ContextShiftConnectionIO: ContextShift[ConnectionIO] =
    connection.ContextShiftConnectionIO

  /** @group Typeclass Instances */  implicit val ContextShiftDatabaseMetaDataIO: ContextShift[DatabaseMetaDataIO] =
    databasemetadata.ContextShiftDatabaseMetaDataIO

  /** @group Typeclass Instances */  implicit val ContextShiftDriverIO: ContextShift[DriverIO] =
    driver.ContextShiftDriverIO

  /** @group Typeclass Instances */  implicit val ContextShiftNClobIO: ContextShift[NClobIO] =
    nclob.ContextShiftNClobIO

  /** @group Typeclass Instances */  implicit val ContextShiftPreparedStatementIO: ContextShift[PreparedStatementIO] =
    preparedstatement.ContextShiftPreparedStatementIO

  /** @group Typeclass Instances */  implicit val ContextShiftRefIO: ContextShift[RefIO] =
    ref.ContextShiftRefIO

  /** @group Typeclass Instances */  implicit val ContextShiftResultSetIO: ContextShift[ResultSetIO] =
    resultset.ContextShiftResultSetIO

  /** @group Typeclass Instances */  implicit val ContextShiftSQLDataIO: ContextShift[SQLDataIO] =
    sqldata.ContextShiftSQLDataIO

  /** @group Typeclass Instances */  implicit val ContextShiftSQLInputIO: ContextShift[SQLInputIO] =
    sqlinput.ContextShiftSQLInputIO

  /** @group Typeclass Instances */  implicit val ContextShiftSQLOutputIO: ContextShift[SQLOutputIO] =
    sqloutput.ContextShiftSQLOutputIO

  /** @group Typeclass Instances */  implicit val ContextShiftStatementIO: ContextShift[StatementIO] =
    statement.ContextShiftStatementIO

  /** @group Typeclass Instances */  implicit val LoggerBlobIO: Logger[BlobIO] =
    blob.LoggerBlobIO

  /** @group Typeclass Instances */  implicit val LoggerCallableStatementIO: Logger[CallableStatementIO] =
    callablestatement.LoggerCallableStatementIO

  /** @group Typeclass Instances */  implicit val LoggerClobIO: Logger[ClobIO] =
    clob.LoggerClobIO

  /** @group Typeclass Instances */  implicit val LoggerConnectionIO: Logger[ConnectionIO] =
    connection.LoggerConnectionIO

  /** @group Typeclass Instances */  implicit val LoggerDatabaseMetaDataIO: Logger[DatabaseMetaDataIO] =
    databasemetadata.LoggerDatabaseMetaDataIO

  /** @group Typeclass Instances */  implicit val LoggerDriverIO: Logger[DriverIO] =
    driver.LoggerDriverIO

  /** @group Typeclass Instances */  implicit val LoggerNClobIO: Logger[NClobIO] =
    nclob.LoggerNClobIO

  /** @group Typeclass Instances */  implicit val LoggerPreparedStatementIO: Logger[PreparedStatementIO] =
    preparedstatement.LoggerPreparedStatementIO

  /** @group Typeclass Instances */  implicit val LoggerRefIO: Logger[RefIO] =
    ref.LoggerRefIO

  /** @group Typeclass Instances */  implicit val LoggerResultSetIO: Logger[ResultSetIO] =
    resultset.LoggerResultSetIO

  /** @group Typeclass Instances */  implicit val LoggerSQLDataIO: Logger[SQLDataIO] =
    sqldata.LoggerSQLDataIO

  /** @group Typeclass Instances */  implicit val LoggerSQLInputIO: Logger[SQLInputIO] =
    sqlinput.LoggerSQLInputIO

  /** @group Typeclass Instances */  implicit val LoggerSQLOutputIO: Logger[SQLOutputIO] =
    sqloutput.LoggerSQLOutputIO

  /** @group Typeclass Instances */  implicit val LoggerStatementIO: Logger[StatementIO] =
    statement.LoggerStatementIO


}
