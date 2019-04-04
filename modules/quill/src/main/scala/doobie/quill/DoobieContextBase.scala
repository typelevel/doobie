// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.quill

import cats.implicits._
import doobie._, doobie.implicits._
import doobie.util.query.DefaultChunkSize
import fs2.Stream
import io.getquill.{ NamingStrategy, DoobieContextBaseStub }
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.StreamingContext
import java.sql.Connection
import scala.reflect.runtime.universe.{ TypeTag, typeTag }
import scala.util.Success

/** Base trait from which vendor-specific variants are derived. */
trait DoobieContextBase[Dialect <: SqlIdiom, Naming <: NamingStrategy]
  extends DoobieContextBaseStub[Dialect, Naming]
     with StreamingContext[Dialect, Naming] {

  type Result[A]               = ConnectionIO[A]
  type RunQueryResult[A]       = List[A]
  type RunQuerySingleResult[A] = List[A]
  type StreamResult[A]         = Stream[ConnectionIO, A]
  type RunActionResult         = Long
  // TODO: type RunActionReturningResult[A]
  // TODO: type RunBatchActionResult
  // TODO: type RunBatchActionReturningResult[A]

  override def executeQuery[A](
    sql:       String,
    prepare:   Prepare      = identityPrepare,
    extractor: Extractor[A] = identityExtractor
  ): ConnectionIO[List[A]] =
    HC.prepareStatement(sql) {
      FPS.raw(prepare) *>
      HPS.executeQuery {
        HRS.list(extractor)
      }
    }

  def streamQuery[A](
    fetchSize: Option[Int],
    sql:       String,
    prepare:   Prepare      = identityPrepare,
    extractor: Extractor[A] = identityExtractor
  ): Stream[ConnectionIO, A] =
    HC.stream(
      sql,
      FPS.raw(prepare).void,
      fetchSize.getOrElse(DefaultChunkSize)
    )(extractor)

  override def executeAction[A](
    sql:     String,
    prepare: Prepare = identityPrepare
  ): ConnectionIO[Long] =
    HC.prepareStatement(sql) {
      FPS.raw(prepare) *>
      HPS.executeUpdate.map(_.toLong)
    }

  // TODO: executeActionReturning
  // TODO: executeBatchAction
  // TODO: executeBatchActionReturning
  // TODO: executeQuerySingle

  // This is very bad. We turn an extractor into a `Read` so we can use the existing resultset
  // machinery. In order to do this we shamelessly stub out the required metadata with nonsense
  // which is ok because it's never used.
  private implicit def extractorToRead[A](ex: Extractor[A]): Read[A] =
    Read.fromGet(Get.Basic.one(null, Nil, (rs, _) => ex(rs))(typeTag[Unit].asInstanceOf[TypeTag[A]]))

  // nothing to do here
  override def close(): Unit = ()

  // nothing to do here either
  override def probe(statement: String) = Success(())

  // can't implement this but it won't be called anyway so ¯\_(ツ)_/¯
  override protected def withConnection[A](f: Connection => ConnectionIO[A]): ConnectionIO[A] = ???

}
