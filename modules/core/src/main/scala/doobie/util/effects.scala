// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.~>
import cats.syntax.flatMap._
import cats.effect.Async
import cats.effect.unsafe.UnsafeRun

object effects {

  def runAsync[F[_], G[_]](implicit F: UnsafeRun[F], G: Async[G]) = λ[F ~> G] { fa =>
    G.delay(F.unsafeRunFutureCancelable(fa)).flatMap { case (running, cancel) =>
      G.executionContext.flatMap(implicit ec =>
        G.async{ k => 
          running.onComplete(t => k(t.toEither))
          G.pure(Some(G.async_(k => cancel().onComplete(t => k(t.toEither)))))
        }
      )
    }
  }

}