// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.util
import cats.syntax.show.*

import java.io.File
import java.nio.file.{InvalidPathException, Path, Paths}

/** Module for source positions. */
object pos {

  /** A source position. `path` is relative to the working directory of the build. If a file is outside of the work
    * directory, `path` will just be the file name
    */
  final case class Pos(path: String, line: Int) {

    def file: String =
      path.lastIndexOf('/') match {
        case -1 => path
        case n  => path.substring(n + 1)
      }

    override def toString =
      show"$file:$line"

  }

  object Pos extends PosPlatform {

    /** Path of a source file relative to the compiler's working directory (the build root under sbt), using `/` as
      * separator. Sources outside of it fall back to the file name, so absolute paths never end up in compiled code.
      * This mirrors how the Scala 3 compiler records source paths in TASTy.
      */
    private[util] def relativePath(path: String, name: String): String =
      relativePath(path, name, Paths.get(""))

    private[util] def relativePath(path: String, name: String, root: Path): String =
      try {
        val absRoot = root.toAbsolutePath.normalize
        val absPath = Paths.get(path).toAbsolutePath.normalize
        if (absPath.startsWith(absRoot)) absRoot.relativize(absPath).toString.replace(File.separatorChar, '/')
        else name
      } catch {
        case _: InvalidPathException => name
      }

  }

}
