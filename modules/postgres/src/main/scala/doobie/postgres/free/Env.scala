// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.free

import io.chrisdavenport.log4cats.Logger

/**
 * Our interpreter environment.
 * @param jdbc the underlying JDBC object (Connection, ResultSet, etc.)
 * @param logger a Log4Cats logger to be used for diagnostic output
 */
final case class Env[F[_], J](
  jdbc:   J,
  logger: Logger[F]
)
