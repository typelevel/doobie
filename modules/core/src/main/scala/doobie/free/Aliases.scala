// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.free

import cats.effect.kernel.{ Sync, MonadCancel }

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

trait Instances  {

  /** @group Typeclass Instances */  implicit lazy val SyncMonadCancelBlobIO: Sync[BlobIO] with MonadCancel[BlobIO, Throwable] =
    blob.SyncMonadCancelBlobIO

  /** @group Typeclass Instances */  implicit lazy val SyncMonadCancelCallableStatementIO: Sync[CallableStatementIO] with MonadCancel[CallableStatementIO, Throwable] =
    callablestatement.SyncMonadCancelCallableStatementIO

  /** @group Typeclass Instances */  implicit lazy val SyncMonadCancelClobIO: Sync[ClobIO] with MonadCancel[ClobIO, Throwable] =
    clob.SyncMonadCancelClobIO

  /** @group Typeclass Instances */  implicit lazy val SyncMonadCancelConnectionIO: Sync[ConnectionIO] with MonadCancel[ConnectionIO, Throwable] =
    connection.SyncMonadCancelConnectionIO

  /** @group Typeclass Instances */  implicit lazy val SyncMonadCancelDatabaseMetaDataIO: Sync[DatabaseMetaDataIO] with MonadCancel[DatabaseMetaDataIO, Throwable] =
    databasemetadata.SyncMonadCancelDatabaseMetaDataIO

  /** @group Typeclass Instances */  implicit lazy val SyncMonadCancelDriverIO: Sync[DriverIO] with MonadCancel[DriverIO, Throwable] =
    driver.SyncMonadCancelDriverIO

  /** @group Typeclass Instances */  implicit lazy val SyncMonadCancelNClobIO: Sync[NClobIO] with MonadCancel[NClobIO, Throwable] =
    nclob.SyncMonadCancelNClobIO

  /** @group Typeclass Instances */  implicit lazy val SyncMonadCancelPreparedStatementIO: Sync[PreparedStatementIO] with MonadCancel[PreparedStatementIO, Throwable] =
    preparedstatement.SyncMonadCancelPreparedStatementIO

  /** @group Typeclass Instances */  implicit lazy val SyncMonadCancelRefIO: Sync[RefIO] with MonadCancel[RefIO, Throwable] =
    ref.SyncMonadCancelRefIO

  /** @group Typeclass Instances */  implicit lazy val SyncMonadCancelResultSetIO: Sync[ResultSetIO] with MonadCancel[ResultSetIO, Throwable] =
    resultset.SyncMonadCancelResultSetIO

  /** @group Typeclass Instances */  implicit lazy val SyncMonadCancelSQLDataIO: Sync[SQLDataIO] with MonadCancel[SQLDataIO, Throwable] =
    sqldata.SyncMonadCancelSQLDataIO

  /** @group Typeclass Instances */  implicit lazy val SyncMonadCancelSQLInputIO: Sync[SQLInputIO] with MonadCancel[SQLInputIO, Throwable] =
    sqlinput.SyncMonadCancelSQLInputIO

  /** @group Typeclass Instances */  implicit lazy val SyncMonadCancelSQLOutputIO: Sync[SQLOutputIO] with MonadCancel[SQLOutputIO, Throwable] =
    sqloutput.SyncMonadCancelSQLOutputIO

  /** @group Typeclass Instances */  implicit lazy val SyncMonadCancelStatementIO: Sync[StatementIO] with MonadCancel[StatementIO, Throwable] =
    statement.SyncMonadCancelStatementIO

}
