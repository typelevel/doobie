// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util.compat

import scala.annotation.implicitNotFound
import scala.collection.Factory
import scala.collection.mutable

// scala-collection-compat seems to require callers to have an import in order to summon its Factory instances
// so we will just define our own
@implicitNotFound(msg = "Cannot construct a factory of type ${C} with elements of type ${A}.")
trait FactoryCompat[-A, +C] {
  def newBuilder: mutable.Builder[A, C]
}

object FactoryCompat {

  implicit def fromFactor[A, C](implicit f: Factory[A, C]): FactoryCompat[A, C] =
    new FactoryCompat[A, C] {
      def newBuilder: mutable.Builder[A, C] = f.newBuilder
    }
}
