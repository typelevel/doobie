// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import magnolia._

trait PutPlatform {
  type Typeclass[T] = Put[T]

  def combine[T](ctx: UnaryReadOnlyCaseClass[Put, T]): Put[T] = {
    val parameter = ctx.parameter
    parameter.typeclass.contramap(t => parameter.dereference(t))
  }

  implicit def generic[A]: Put[A] = macro Magnolia.gen[A]
}
