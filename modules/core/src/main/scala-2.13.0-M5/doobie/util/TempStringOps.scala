// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.collection.Iterator

// `linesIterator` is added back on the 2.13 branch but it not in any released version
// TODO remove this file when upgrading to a new 2.13 release; a compile error will prompt this
final class TempStringOps(s: String) {
  import scala.Predef.augmentString

  // a copy of `lines` from scala.collection.StringOps
  def linesIterator: Iterator[String] = s.linesWithSeparators.map(_.stripLineEnd)
}

trait ToTempStringOps {
  implicit def toTempStringOps(s: String): TempStringOps = new TempStringOps(s)
}
