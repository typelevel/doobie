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
import doobie.util.Read

import scala.collection.immutable

class FragmentOps(fragment: Fragment) {

  /** Build a query labeled with JSON-encoded attributes for tracing.
    *
    * @note
    *   the label is interpreted as attributes only when
    *   [[doobie.otel4s.TracedInterpreter.CaptureConfig.CaptureLabel.decodeAttributes]] is enabled.
    */
  def queryWithAttributes[A: Read](attributes: immutable.Iterable[Attribute[?]]): Query0[A] =
    fragment.queryWithLabel(attributes.asJson.noSpaces)

  /** Build a query labeled with JSON-encoded attributes for tracing.
    *
    * @note
    *   the label is interpreted as attributes only when
    *   [[doobie.otel4s.TracedInterpreter.CaptureConfig.CaptureLabel.decodeAttributes]] is enabled.
    */
  def queryWithAttributes[A: Read](attributes: Attribute[?]*): Query0[A] =
    fragment.queryWithLabel(attributes.asJson.noSpaces)

  /** Build an update labeled with JSON-encoded attributes for tracing.
    *
    * @note
    *   the label is interpreted as attributes only when
    *   [[doobie.otel4s.TracedInterpreter.CaptureConfig.CaptureLabel.decodeAttributes]] is enabled.
    */
  def updateWithAttributes(attributes: immutable.Iterable[Attribute[?]]): Update0 =
    fragment.updateWithLabel(attributes.asJson.noSpaces)

  /** Build an update labeled with JSON-encoded attributes for tracing.
    *
    * @note
    *   the label is interpreted as attributes only when
    *   [[doobie.otel4s.TracedInterpreter.CaptureConfig.CaptureLabel.decodeAttributes]] is enabled.
    */
  def updateWithAttributes(attributes: Attribute[?]*): Update0 =
    fragment.updateWithLabel(attributes.asJson.noSpaces)

}

trait ToFragmentOps {
  implicit def toFragmentOps(f: Fragment): FragmentOps =
    new FragmentOps(f)
}

object fragment extends ToFragmentOps
