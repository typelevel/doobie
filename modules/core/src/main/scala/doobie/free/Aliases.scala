// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.free

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

