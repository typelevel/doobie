// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import magnolia.{CaseClass, Magnolia}
import java.sql.ResultSet

trait ReadPlatform  { this: Read.type =>
  type Typeclass[T] = Read[T]

  def combine[T](ctx: CaseClass[Read, T]): Read[T] = {
    val gets   = ctx.parameters.toList.flatMap(_.typeclass.gets)
    val offset = ctx.parameters.scanLeft(0)(_ + _.typeclass.gets.length)

    def unsafeGet(rs: ResultSet, index: Int): T =
      ctx.construct(p => p.typeclass.unsafeGet(rs, index + offset(p.index)))

    def unsafeGetOption(rs: ResultSet, index: Int): Option[T] =
      ctx.constructMonadic(p => p.typeclass.unsafeGetOption(rs, index + offset(p.index)))

    new Read(gets, unsafeGet, unsafeGetOption)
  }

  implicit def generic[A]: Read[A] = macro Magnolia.gen[A]
}


