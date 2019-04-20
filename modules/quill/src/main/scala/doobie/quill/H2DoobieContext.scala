// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.quill

import io.getquill.{ NamingStrategy, H2Dialect }
import io.getquill.context.jdbc.H2JdbcContextBase

class H2DoobieContext[N <: NamingStrategy](val naming: N)
  extends DoobieContextBase[H2Dialect, N]
     with H2JdbcContextBase[N]
