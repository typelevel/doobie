// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package io.getquill

import io.getquill.context.jdbc.JdbcContextBase
import io.getquill.context.sql.idiom.SqlIdiom

// We need to implement a little stub here in `io.getquill` because `effect` is package-private.
// This will probably go away in the future. Note we're not using `extractResults` (also package-
// private) anymore so it's just this one thing now.
trait DoobieContextBaseStub[Dialect <: SqlIdiom, Naming <: NamingStrategy]
  extends JdbcContextBase[Dialect, Naming] {

  // This will never be called anyway.
  private[getquill] val effect = null

}
