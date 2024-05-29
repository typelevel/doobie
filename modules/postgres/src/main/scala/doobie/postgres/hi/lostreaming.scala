// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.hi

import cats.syntax.functor._
import doobie.ConnectionIO
import doobie.implicits._
import doobie.postgres.free.{largeobjectmanager => IIPFLOM, largeobject => IPFLO}
import doobie.postgres.hi.{connection => IPHC}
import fs2.Stream
import java.io.{InputStream, OutputStream}
import org.postgresql.largeobject.LargeObject

object lostreaming {

  def createLOFromStream(data: Stream[ConnectionIO, Byte]): ConnectionIO[Long] =
    createLO.flatMap { oid =>
      Stream.bracket(openLO(oid))(closeLO)
        .flatMap(lo => data.through(fs2.io.writeOutputStream(getOutputStream(lo))))
        .compile.drain.as(oid)
    }

  def createStreamFromLO(oid: Long, chunkSize: Int): Stream[ConnectionIO, Byte] =
    Stream.bracket(openLO(oid))(closeLO)
      .flatMap(lo => fs2.io.readInputStream(getInputStream(lo), chunkSize))

  private val createLO: ConnectionIO[Long] =
    IPHC.pgGetLargeObjectAPI(IIPFLOM.createLO)

  private def openLO(oid: Long): ConnectionIO[LargeObject] =
    IPHC.pgGetLargeObjectAPI(IIPFLOM.open(oid))

  private def closeLO(lo: LargeObject): ConnectionIO[Unit] =
    IPHC.pgGetLargeObjectAPI(IIPFLOM.embed(lo, IPFLO.close))

  private def getOutputStream(lo: LargeObject): ConnectionIO[OutputStream] =
    IPHC.pgGetLargeObjectAPI(IIPFLOM.embed(lo, IPFLO.getOutputStream))

  private def getInputStream(lo: LargeObject): ConnectionIO[InputStream] =
    IPHC.pgGetLargeObjectAPI(IIPFLOM.embed(lo, IPFLO.getInputStream))
}
