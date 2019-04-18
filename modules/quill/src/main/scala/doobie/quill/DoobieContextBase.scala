// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.quill

import cats.data.Nested
import cats.effect.Resource
import cats.implicits._
import doobie._, doobie.implicits._
import doobie.util.query.DefaultChunkSize
import fs2.Stream
import io.getquill.{ NamingStrategy, DoobieContextBaseStub }
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.StreamingContext
import java.sql.{ Connection }
import scala.reflect.runtime.universe.{ TypeTag, typeTag }
import scala.util.Success

/** Base trait from which vendor-specific variants are derived. */
trait DoobieContextBase[Dialect <: SqlIdiom, Naming <: NamingStrategy]
  extends DoobieContextBaseStub[Dialect, Naming]
     with StreamingContext[Dialect, Naming] {

  type Result[A]                        = ConnectionIO[A]
  type RunQueryResult[A]                = List[A]
  type RunQuerySingleResult[A]          = A
  type StreamResult[A]                  = Stream[ConnectionIO, A]
  type RunActionResult                  = Long
  type RunActionReturningResult[A]      = A
  type RunBatchActionResult             = List[Long]
  type RunBatchActionReturningResult[A] = List[A]

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

  override def executeQuerySingle[A](
    sql:       String,
    prepare:   Prepare      = identityPrepare,
    extractor: Extractor[A] = identityExtractor
  ): ConnectionIO[A] =
    HC.prepareStatement(sql) {
      FPS.raw(prepare) *>
      HPS.executeQuery {
        HRS.getUnique(extractor)
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

  override def executeActionReturning[A](
    sql:       String,
    prepare:   Prepare = identityPrepare,
    extractor: Extractor[A],
    column:    String
  ): ConnectionIO[A] =
    HC.prepareStatementS(sql, List(column)) {
      FPS.raw(prepare)  *>
      FPS.executeUpdate *>
      HPS.getGeneratedKeys(HRS.getUnique(extractor))
    }

  override def executeBatchAction(
    groups: List[BatchGroup]
  ): ConnectionIO[List[Long]] =
    groups.flatTraverse { case BatchGroup(sql, preps) =>
      HC.prepareStatement(sql) {
        preps.traverse(FPS.raw(_) *> FPS.addBatch) *>
        Nested(HPS.executeBatch).map(_.toLong).value
      }
    }

  override def executeBatchActionReturning[A](
    groups:    List[BatchGroupReturning],
    extractor: Extractor[A]
  ): ConnectionIO[List[A]] =
    groups.flatTraverse { case BatchGroupReturning(sql, column, preps) =>
      HC.prepareStatementS(sql, List(column)) {
        preps.traverse(FPS.raw(_) *> FPS.addBatch) *>
        HPS.executeBatch *>
        HPS.getGeneratedKeys(HRS.list(extractor))
      }
    }

  // This is very bad. We turn an extractor into a `Read` so we can use the existing resultset
  // machinery. In order to do this we shamelessly stub out the required metadata with nonsense
  // which is ok because it's never used, heh-heh.
  private implicit def extractorToRead[A](ex: Extractor[A]): Read[A] =
    Read.fromGet(Get.Basic.one(null, Nil, (rs, _) => ex(rs))(typeTag[Unit].asInstanceOf[TypeTag[A]]))

  // Nothing to do here.
  override def close(): Unit = ()

  // Nothing to do here either.
  override def probe(statement: String) = Success(())

  // We can't implement this but it won't be called anyway so ¯\_(ツ)_/¯
  override protected def withConnection[A](f: Connection => ConnectionIO[A]): ConnectionIO[A] = ???

}
