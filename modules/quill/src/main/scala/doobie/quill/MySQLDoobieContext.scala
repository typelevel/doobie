// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.quill

import io.getquill.{ NamingStrategy, MySQLDialect }
import io.getquill.context.jdbc.MysqlJdbcContextBase

class MySQLDoobieContext[N <: NamingStrategy](val naming: N)
  extends DoobieContextBase[MySQLDialect, N]
     with MysqlJdbcContextBase[N]
