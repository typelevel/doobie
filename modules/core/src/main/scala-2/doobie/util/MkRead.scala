// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import doobie.enumerated.Nullability.*

import java.sql.ResultSet
final class MkRead[A](val underlying: Read[A]) extends Read[A] {
  override def unsafeGet(rs: ResultSet, startIdx: Int): A = underlying.unsafeGet(rs, startIdx)
  override def gets: List[(Get[?], NullabilityKnown)] = underlying.gets
  override def toOpt: Read[Option[A]] = underlying.toOpt
  override def length: Int = underlying.length
}

object MkRead extends MkReadInstances

trait MkReadInstances extends MkReadPlatform
