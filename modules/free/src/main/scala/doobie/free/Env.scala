// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.free

import org.slf4j.Logger

/**
 * Interpreter environment.
 * @param jdbc The JDBC managed object (Connection, Statement, etc.)
 * @param logger A logger for tracing execution.
 */
final case class Env[J](
  jdbc:   J,
  logger: Logger
)
