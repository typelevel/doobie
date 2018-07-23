// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.linebacker

import io.chrisdavenport.linebacker.Linebacker
import cats.effect.Async
import doobie._
import java.sql.Connection
import doobie.free.connection._
import cats._
import cats.data._
import scala.concurrent.ExecutionContext

object LinebackerTransactor {
  def apply[F[_]: Async: Linebacker](t: Transactor[F])(implicit cpuEc: ExecutionContext) : Transactor[F] = {
    val blockingInterpreter = new ~>[ConnectionOp, Kleisli[F, Connection, ?]]{
      def apply[A](fa: ConnectionOp[A]): Kleisli[F, Connection,A] = 
        Kleisli{ connection: Connection => 
          Linebacker[F].block(t.interpret(fa).run(connection))
        }
    }
    t.copy(interpret0 = blockingInterpreter)
  }
}