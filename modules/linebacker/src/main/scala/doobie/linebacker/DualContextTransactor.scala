// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.linebacker

import io.chrisdavenport.linebacker.DualContext
import cats.effect.Async
import doobie._
import java.sql.Connection
import doobie.free.connection._
import cats._
import cats.data._

object DualContextTransactor {
  def apply[F[_]: Async](dc: DualContext[F])(t: Transactor[F]): Transactor[F] = {
    val blockingInterpreter = new ~>[ConnectionOp, Kleisli[F, Connection, ?]]{
      def apply[A](fa: ConnectionOp[A]): Kleisli[F, Connection,A] = 
        Kleisli{ connection: Connection => 
          dc.block(t.interpret(fa).run(connection))
        }
    }
    t.copy(interpret0 = blockingInterpreter)
  }
}