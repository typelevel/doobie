// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s

import cats.effect.{Async, LiftIO}
import cats.syntax.functor.*
import doobie.Transactor
import doobie.util.log.LogHandler
import org.typelevel.otel4s.trace.TracerProvider

object TracedTransactor {

  /** Returns a transactor that uses a traced interpreter.
    *
    * If no log handler is supplied, a no-op handler is used.
    */
  def create[F[_]: Async: TracerProvider: LiftIO](
      transactor: Transactor[F],
      config: TracingConfig,
      logHandler: Option[LogHandler[F]]
  ): F[Transactor[F]] =
    for {
      interpreter <- TracedInterpreter.create[F](config, logHandler.getOrElse(LogHandler.noop))
    } yield Transactor.interpret.set(transactor, interpreter.ConnectionInterpreter)

}
