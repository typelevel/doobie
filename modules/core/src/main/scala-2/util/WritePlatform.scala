// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import magnolia._
import java.sql.{PreparedStatement, ResultSet}

trait WritePlatform {
  type Typeclass[A] = Write[A]

  def combine[A](ctx: ReadOnlyCaseClass[Write, A]): Write[A] = {
    val puts         = ctx.parameters.toList.flatMap(_.typeclass.puts)
    val offset       = ctx.parameters.scanLeft(0)(_ + _.typeclass.puts.length)
    def toList(a: A) = ctx.parameters.toList.flatMap(param => param.typeclass.toList(param.dereference(a)))

    def unsafeSet(ps: PreparedStatement, n: Int, a: A) =
      ctx.parameters.foreach(param => param.typeclass.unsafeSet(ps, n + offset(param.index), param.dereference(a)))

    def unsafeUpdate(rs: ResultSet, n: Int, a: A) =
      ctx.parameters.foreach(param => param.typeclass.unsafeUpdate(rs, n + offset(param.index), param.dereference(a)))

    def unsafeSetOption(ps: PreparedStatement, n: Int, oa: Option[A]) =
      ctx.parameters.foreach(param => param.typeclass.unsafeSetOption(ps, n + offset(param.index), oa.map(param.dereference)))

    def unsafeUpdateOption(rs: ResultSet, n: Int, oa: Option[A]) =
      ctx.parameters.foreach(param => param.typeclass.unsafeUpdateOption(rs, n + offset(param.index), oa.map(param.dereference)))

    new Write(puts, toList, unsafeSet, unsafeUpdate, unsafeSetOption, unsafeUpdateOption)
  }

  implicit def generic[A]: Write[A] = macro Magnolia.gen[A]
}
