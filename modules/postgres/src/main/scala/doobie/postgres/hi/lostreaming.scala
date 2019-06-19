// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.hi

import cats.effect.Blocker
import cats.syntax.functor._
import doobie.ConnectionIO
import doobie.implicits.AsyncConnectionIO
import fs2.Stream
import java.io.{InputStream, OutputStream}
import org.postgresql.largeobject.LargeObject

object lostreaming {

  def createLOFromStream(data: Stream[ConnectionIO, Byte], blocker: Blocker): ConnectionIO[Long] =
    createLO.flatMap { oid =>
      Stream.bracket(openLO(oid))(closeLO)
        .flatMap(lo => data.through(fs2.io.writeOutputStream(getOutputStream(lo), blocker)))
        .compile.drain.as(oid)
    }

  def createStreamFromLO(oid: Long, chunkSize: Int, blocker: Blocker): Stream[ConnectionIO, Byte] =
    Stream.bracket(openLO(oid))(closeLO)
      .flatMap(lo => fs2.io.readInputStream(getInputStream(lo), chunkSize, blocker))

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
