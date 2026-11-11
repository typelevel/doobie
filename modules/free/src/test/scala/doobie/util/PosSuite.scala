// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.util

import org.typelevel.doobie.util.pos.Pos

import java.nio.file.Paths

class PosSuite extends munit.FunSuite {

  test("derived Pos has a path relative to the build root") {
    val pos = implicitly[Pos]
    assertEquals(pos.path, "modules/free/src/test/scala/doobie/util/PosSuite.scala")
    assertEquals(pos.file, "PosSuite.scala")
  }

  test("relativePath uses / as separator for sources under the root") {
    val root = Paths.get("some", "root")
    val path = root.resolve(Paths.get("a", "b", "C.scala")).toAbsolutePath.toString
    assertEquals(Pos.relativePath(path, "C.scala", root), "a/b/C.scala")
  }

  test("relativePath falls back to the file name for sources outside of the root") {
    val root = Paths.get("some", "root")
    val path = Paths.get("elsewhere", "C.scala").toAbsolutePath.toString
    assertEquals(Pos.relativePath(path, "C.scala", root), "C.scala")
  }

}
