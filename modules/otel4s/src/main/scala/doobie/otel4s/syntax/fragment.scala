// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s.syntax

import doobie.Fragment
import doobie.util.query.Query0
import doobie.util.update.Update0
import org.typelevel.otel4s.Attribute
import io.circe.syntax.*
import doobie.otel4s.AttributesCodec.*
import doobie.otel4s.syntax.all.*
import doobie.util.Read
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

  /** Build a query with encoded tracing attributes payload.
    *
    * @note
    *   these attributes are encoded as an internal label payload and interpreted by
    *   [[doobie.otel4s.AttributesExtractor.json]].
    */
  def queryWithAttributes[A: Read](attributes: immutable.Iterable[Attribute[?]]): Query0[A] =
    fragment.queryWithLabel(attributes.asJson.noSpaces)

  /** Build a query with encoded tracing attributes payload.
    *
    * @note
    *   these attributes are encoded as an internal label payload and interpreted by
    *   [[doobie.otel4s.AttributesExtractor.json]].
    */
  def queryWithAttributes[A: Read](attributes: Attribute[?]*): Query0[A] =
    fragment.queryWithLabel(attributes.asJson.noSpaces)

  /** Build an update with encoded tracing attributes payload.
    *
    * @note
    *   these attributes are encoded as an internal label payload and interpreted by
    *   [[doobie.otel4s.AttributesExtractor.json]].
    */
  def updateWithAttributes(attributes: immutable.Iterable[Attribute[?]]): Update0 =
    fragment.updateWithLabel(attributes.asJson.noSpaces)

  /** Build an update with encoded tracing attributes payload.
    *
    * @note
    *   these attributes are encoded as an internal label payload and interpreted by
    *   [[doobie.otel4s.AttributesExtractor.json]].
    */
  def updateWithAttributes(attributes: Attribute[?]*): Update0 =
    fragment.updateWithLabel(attributes.asJson.noSpaces)

}

trait ToFragmentOps {
  implicit def toFragmentOps(f: Fragment): FragmentOps =
    new FragmentOps(f)
}

object fragment extends ToFragmentOps
