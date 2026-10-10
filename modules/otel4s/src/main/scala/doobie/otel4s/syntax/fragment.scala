// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.otel4s.syntax

import org.typelevel.doobie.Fragment
import org.typelevel.doobie.util.query.Query0
import org.typelevel.doobie.util.update.Update0
import org.typelevel.otel4s.{Attribute, Attributes}
import org.typelevel.doobie.otel4s.AttributesMetadata
import org.typelevel.doobie.otel4s.syntax.all.*
import org.typelevel.doobie.util.Read
import org.typelevel.otel4s.semconv.attributes.DbAttributes

import scala.collection.immutable

class FragmentOps(fragment: Fragment) {

  /** Build a query with explicit summary for tracing.
    */
  def queryWithSummary[A: Read](summary: String): Query0[A] =
    fragment.queryWithAttributes(DbAttributes.DbQuerySummary(summary))

  /** Build an update with explicit summary for tracing.
    */
  def updateWithSummary(summary: String): Update0 =
    fragment.updateWithAttributes(DbAttributes.DbQuerySummary(summary))

  /** Build a query with tracing attributes stored in statement metadata. */
  def queryWithAttributes[A: Read](attributes: immutable.Iterable[Attribute[?]]): Query0[A] =
    fragment.query[A].withMetadata(AttributesMetadata.key, Attributes.fromSpecific(attributes))

  /** Build a query with tracing attributes stored in statement metadata. */
  def queryWithAttributes[A: Read](attributes: Attribute[?]*): Query0[A] =
    queryWithAttributes[A](Attributes.fromSpecific(attributes))

  /** Build an update with tracing attributes stored in statement metadata. */
  def updateWithAttributes(attributes: immutable.Iterable[Attribute[?]]): Update0 =
    fragment.update.withMetadata(AttributesMetadata.key, Attributes.fromSpecific(attributes))

  /** Build an update with tracing attributes stored in statement metadata. */
  def updateWithAttributes(attributes: Attribute[?]*): Update0 =
    updateWithAttributes(Attributes.fromSpecific(attributes.toVector))

}

trait ToFragmentOps {
  implicit def toFragmentOps(f: Fragment): FragmentOps =
    new FragmentOps(f)
}

object fragment extends ToFragmentOps
