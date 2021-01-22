// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import magnolia._

import org.tpolecat.typename.TypeName

trait GetPlatform {
  type Typeclass[T] = Get[T]

  def combine[T: TypeName: Unary](ctx: CaseClass[Get, T]): Get[T] =
    ctx.parameters.head.typeclass.tmap[T](ptype => ctx.rawConstruct(List(ptype)))

  implicit def generic[A]: Get[A] = macro Magnolia.gen[A]
}

