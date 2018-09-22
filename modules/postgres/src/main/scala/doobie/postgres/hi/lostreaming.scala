// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.hi

import cats.effect.ContextShift
import cats.implicits._
import doobie._, doobie.implicits._
import fs2.Stream
import fs2.{io => FS2IO}
import java.io.{InputStream, OutputStream}
import org.postgresql.largeobject.LargeObject
import scala.concurrent.ExecutionContext

object lostreaming {
  def createLOFromStream(data: Stream[ConnectionIO, Byte], blockingExecutionContext: ExecutionContext)(
    implicit cs: ContextShift[ConnectionIO]
  ): ConnectionIO[Long] =
    createLO.flatMap { oid =>
      Stream.bracket(openLO(oid))(closeLO)
        .flatMap(lo => data.to(FS2IO.writeOutputStream(getOutputStream(lo), blockingExecutionContext)))
        .compile.drain.as(oid)
    }

  def createStreamFromLO(oid: Long, chunkSize: Int, blockingExecutionContext: ExecutionContext)(
    implicit cs: ContextShift[ConnectionIO]
  ): Stream[ConnectionIO, Byte] =
    Stream.bracket(openLO(oid))(closeLO)
      .flatMap(lo => FS2IO.readInputStream(getInputStream(lo), chunkSize, blockingExecutionContext))

  private val createLO: ConnectionIO[Long] =
    PHC.pgGetLargeObjectAPI(PFLOM.createLO)

  private def openLO(oid: Long): ConnectionIO[LargeObject] =
    PHC.pgGetLargeObjectAPI(PFLOM.open(oid))

  private def closeLO(lo: LargeObject): ConnectionIO[Unit] =
    PHC.pgGetLargeObjectAPI(PFLOM.embed(lo, PFLO.close))

  private def getOutputStream(lo: LargeObject): ConnectionIO[OutputStream] =
    PHC.pgGetLargeObjectAPI(PFLOM.embed(lo, PFLO.getOutputStream))

  private def getInputStream(lo: LargeObject): ConnectionIO[InputStream] =
    PHC.pgGetLargeObjectAPI(PFLOM.embed(lo, PFLO.getInputStream))
}
