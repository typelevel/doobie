// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s

import cats.syntax.functor.*
import doobie.{Transactor, WeakAsync}
import doobie.util.log.LogHandler
import org.typelevel.otel4s.trace.TracerProvider

object TracedTransactor {

  def create[F[_]: WeakAsync: TracerProvider](
      transactor: Transactor[F],
      config: TracedInterpreter.Config,
      logHandler: Option[LogHandler[F]]
  ): F[Transactor[F]] =
    for {
      interpreter <- TracedInterpreter.create[F](config, logHandler.getOrElse(LogHandler.noop))
    } yield Transactor.interpret.set(transactor, interpreter.ConnectionInterpreter)

}
