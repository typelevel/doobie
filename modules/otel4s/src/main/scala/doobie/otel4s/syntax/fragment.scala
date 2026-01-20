// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s.syntax

import doobie.Fragment
import doobie.util.query.Query0
import doobie.util.update.Update0
import org.typelevel.otel4s.Attributes
import io.circe.syntax._
import doobie.otel4s.AttributesCodec._
import doobie.util.Read

class FragmentOps(fragment: Fragment) {

  def queryWithAttributes[A: Read](attributes: Attributes): Query0[A] =
    fragment.queryWithLabel(attributes.asJson.noSpaces)

  def updateWithAttributes(attributes: Attributes): Update0 =
    fragment.updateWithLabel(attributes.asJson.noSpaces)

}

trait ToFragmentOps {
  implicit def toFragmentOps(f: Fragment): FragmentOps =
    new FragmentOps(f)
}

object fragment extends ToFragmentOps
