// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.otel4s.syntax

import org.typelevel.doobie.otel4s.AttributesMetadata
import org.typelevel.doobie.util.query.{Query, Query0}
import org.typelevel.doobie.util.update.{Update, Update0}
import org.typelevel.otel4s.{Attribute, Attributes}

import scala.collection.immutable

class QueryOps[A, B](query: Query[A, B]) {
  def withAttributes(attributes: immutable.Iterable[Attribute[?]]): Query[A, B] =
    query.withMetadata(AttributesMetadata.add(query.metadata, Attributes.fromSpecific(attributes)))

  def withAttributes(attributes: Attribute[?]*): Query[A, B] =
    withAttributes(attributes)
}

class Query0Ops[A](query: Query0[A]) {
  def withAttributes(attributes: immutable.Iterable[Attribute[?]]): Query0[A] =
    query.withMetadata(AttributesMetadata.add(query.metadata, Attributes.fromSpecific(attributes)))

  def withAttributes(attributes: Attribute[?]*): Query0[A] =
    withAttributes(Attributes.fromSpecific(attributes))
}

class UpdateOps[A](update: Update[A]) {
  def withAttributes(attributes: immutable.Iterable[Attribute[?]]): Update[A] =
    update.withMetadata(AttributesMetadata.add(update.metadata, Attributes.fromSpecific(attributes)))

  def withAttributes(attributes: Attribute[?]*): Update[A] =
    withAttributes(Attributes.fromSpecific(attributes))
}

class Update0Ops(update: Update0) {
  def withAttributes(attributes: immutable.Iterable[Attribute[?]]): Update0 =
    update.withMetadata(AttributesMetadata.add(update.metadata, Attributes.fromSpecific(attributes)))

  def withAttributes(attributes: Attribute[?]*): Update0 =
    withAttributes(Attributes.fromSpecific(attributes))
}

trait ToStatementOps {
  implicit def toQueryOps[A, B](query: Query[A, B]): QueryOps[A, B] = new QueryOps(query)
  implicit def toQuery0Ops[A](query: Query0[A]): Query0Ops[A] = new Query0Ops(query)
  implicit def toUpdateOps[A](update: Update[A]): UpdateOps[A] = new UpdateOps(update)
  implicit def toUpdate0Ops(update: Update0): Update0Ops = new Update0Ops(update)
}

object statement extends ToStatementOps
