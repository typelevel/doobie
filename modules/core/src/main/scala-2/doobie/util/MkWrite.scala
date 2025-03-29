// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import doobie.enumerated.Nullability.*

import java.sql.{PreparedStatement, ResultSet}

final class MkWrite[A](val instance: Write[A]) extends Write[A] {
  override def puts: List[(Put[?], NullabilityKnown)] = instance.puts
  override def toList(a: A): List[Any] = instance.toList(a)
  override def unsafeSet(ps: PreparedStatement, startIdx: Int, a: A): Unit = instance.unsafeSet(ps, startIdx, a)
  override def unsafeUpdate(rs: ResultSet, startIdx: Int, a: A): Unit = instance.unsafeUpdate(rs, startIdx, a)
  override def toOpt: Write[Option[A]] = instance.toOpt
  override def length: Int = instance.length
}

object MkWrite extends MkWriteInstances

trait MkWriteInstances extends MkWritePlatform
