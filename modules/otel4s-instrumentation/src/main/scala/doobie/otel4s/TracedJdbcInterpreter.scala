// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s

import cats.data.Kleisli
import cats.mtl.Ask
import cats.syntax.flatMap.*
import doobie.WeakAsync
import doobie.free.KleisliInterpreter
import doobie.util.log.LogHandler
import org.typelevel.otel4s.oteljava.context.{AskContext, Context}

private class TracedJdbcInterpreter[F[_]: WeakAsync: AskContext](
    logHandler: LogHandler[F]
) extends KleisliInterpreter(logHandler) {

  override def primitive[J, A](f: J => A): Kleisli[F, J, A] = Kleisli { a =>
    Ask[F, Context].ask.flatMap { ctx =>
      // primitive JDBC methods throw exceptions and so do we when reading values
      // so catch any non-fatal exceptions and lift them into the effect
      try {
        asyncM.blocking {
          // make the current context active
          val scope = ctx.underlying.makeCurrent()
          try {
            f(a)
          } finally {
            scope.close() // release the context
          }
        }
      } catch {
        case scala.util.control.NonFatal(e) => asyncM.raiseError(e)
      }
    }
  }

}

object TracedJdbcInterpreter {

  def apply[F[_]: WeakAsync: AskContext](logHandler: LogHandler[F]): KleisliInterpreter[F] =
    new TracedJdbcInterpreter(logHandler)

}
