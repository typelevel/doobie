// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.free

import cats.effect.Async
import cats.effect._
import scala.concurrent.ExecutionContext

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


  /**
   * We can piggyback on ContextShift[IO] if we have LiftIO and Bracket.
   * @group Derivations
   */
  def contextShiftViaLiftIO[F[_]](
    implicit cs: ContextShift[IO],
             io: LiftIO[F],
             br: Bracket[F, Throwable]
  ): ContextShift[F] =
    new ContextShift[F] {
      def shift: F[Unit] = io.liftIO(cs.shift)
      def evalOn[A](ec: ExecutionContext)(fa: F[A]): F[A] =
        br.bracket(io.liftIO(IO.shift(ec)))(_ => fa)(_ => io.liftIO(cs.shift))
    }

  /** @group Typeclass Instances */ implicit def contextShiftBlobIO(implicit ev: ContextShift[IO]): ContextShift[BlobIO] = contextShiftViaLiftIO
  /** @group Typeclass Instances */ implicit def contextShiftCallableStatementIO(implicit ev: ContextShift[IO]): ContextShift[CallableStatementIO] = contextShiftViaLiftIO
  /** @group Typeclass Instances */ implicit def contextShiftClobIO(implicit ev: ContextShift[IO]): ContextShift[ClobIO] = contextShiftViaLiftIO
  /** @group Typeclass Instances */ implicit def contextShiftConnectionIO(implicit ev: ContextShift[IO]): ContextShift[ConnectionIO] = contextShiftViaLiftIO
  /** @group Typeclass Instances */ implicit def contextShiftDatabaseMetaDataIO(implicit ev: ContextShift[IO]): ContextShift[DatabaseMetaDataIO] = contextShiftViaLiftIO
  /** @group Typeclass Instances */ implicit def contextShiftDriverIO(implicit ev: ContextShift[IO]): ContextShift[DriverIO] = contextShiftViaLiftIO
  /** @group Typeclass Instances */ implicit def contextShiftNClobIO(implicit ev: ContextShift[IO]): ContextShift[NClobIO] = contextShiftViaLiftIO
  /** @group Typeclass Instances */ implicit def contextShiftPreparedStatementIO(implicit ev: ContextShift[IO]): ContextShift[PreparedStatementIO] = contextShiftViaLiftIO
  /** @group Typeclass Instances */ implicit def contextShiftRefIO(implicit ev: ContextShift[IO]): ContextShift[RefIO] = contextShiftViaLiftIO
  /** @group Typeclass Instances */ implicit def contextShiftResultSetIO(implicit ev: ContextShift[IO]): ContextShift[ResultSetIO] = contextShiftViaLiftIO
  /** @group Typeclass Instances */ implicit def contextShiftSQLDataIO(implicit ev: ContextShift[IO]): ContextShift[SQLDataIO] = contextShiftViaLiftIO
  /** @group Typeclass Instances */ implicit def contextShiftSQLInputIO(implicit ev: ContextShift[IO]): ContextShift[SQLInputIO] = contextShiftViaLiftIO
  /** @group Typeclass Instances */ implicit def contextShiftSQLOutputIO(implicit ev: ContextShift[IO]): ContextShift[SQLOutputIO] = contextShiftViaLiftIO
  /** @group Typeclass Instances */ implicit def contextShiftStatementIO(implicit ev: ContextShift[IO]): ContextShift[StatementIO] = contextShiftViaLiftIO

}
