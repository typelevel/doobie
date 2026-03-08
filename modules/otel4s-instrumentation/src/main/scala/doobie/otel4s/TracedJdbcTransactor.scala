// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s

import javax.sql.DataSource

import doobie.util.log.LogHandler
import doobie.{Transactor, WeakAsync}
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry
import org.typelevel.otel4s.oteljava.context.AskContext

object TracedJdbcTransactor {

  def dataSource[F[_]: WeakAsync: AskContext](
      jdbcTelemetry: JdbcTelemetry,
      transactor: Transactor.Aux[F, DataSource],
      logHandler: Option[LogHandler[F]]
  ): Transactor.Aux[F, DataSource] = {
    val interpreter = new TracedJdbcInterpreter[F](logHandler.getOrElse(LogHandler.noop))
    transactor.copy(
      kernel0 = jdbcTelemetry.wrap(transactor.kernel),
      interpret0 = interpreter.ConnectionInterpreter
    )
  }

}
