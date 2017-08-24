// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.hi

trait Modules {
  lazy val HC  = doobie.hi.connection
  lazy val HS  = doobie.hi.statement
  lazy val HPS = doobie.hi.preparedstatement
  lazy val HRS = doobie.hi.resultset
}
