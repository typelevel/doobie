package doobie.contrib.postgresql.hi

import doobie.imports._
import doobie.util.io.IOActions
import doobie.contrib.postgresql.imports._

import java.io.File

#+scalaz
import scalaz.syntax.apply._
#-scalaz

object largeobject {

  lazy val io = new IOActions[LargeObjectIO]

#+scalaz
  def copyFromFile(blockSize: Int, file: File): LargeObjectIO[Unit] =
    PFLO.getOutputStream.flatMap { os => io.copyFileToStream(blockSize, file, os) *> io.flush(os) }

  def copyToFile(blockSize: Int, file: File): LargeObjectIO[Unit] =
    PFLO.getInputStream.flatMap { is => io.copyStreamToFile(blockSize, file, is) }
#-scalaz

}
