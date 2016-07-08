package doobie.contrib.postgresql.hi

import doobie.imports._
import doobie.util.io.IOActions
import doobie.contrib.postgresql.imports._

import java.io.File

import scalaz.syntax.monad._

object largeobject {

  lazy val io = new IOActions[LargeObjectIO]

  def copyFromFile(blockSize: Int, file: File): LargeObjectIO[Unit] =
    PFLO.getOutputStream >>= { os => io.copyFileToStream(blockSize, file, os) *> io.flush(os) }

  def copyToFile(blockSize: Int, file: File): LargeObjectIO[Unit] =
    PFLO.getInputStream >>= { is => io.copyStreamToFile(blockSize, file, is) }

}
