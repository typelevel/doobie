// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.quill

import io.getquill.{ NamingStrategy, SqliteDialect }
import io.getquill.context.jdbc.SqliteJdbcContextBase

class SQLiteDoobieContext[N <: NamingStrategy](val naming: N)
  extends DoobieContextBase[SqliteDialect, N]
     with SqliteJdbcContextBase[N]
