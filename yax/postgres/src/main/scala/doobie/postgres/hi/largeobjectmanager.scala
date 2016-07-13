package doobie.postgres.hi

import doobie.imports._
import doobie.postgres.imports._

import java.io.File

#+scalaz
import scalaz.syntax.monad._
#-scalaz
#+cats
import cats.implicits._
#-cats

object largeobjectmanager {

  val createLO: LargeObjectManagerIO[Long] =
    PFLOM.createLO

  def createLO(a: Int): LargeObjectManagerIO[Long] =
    PFLOM.createLO(a)

  def delete(a: Long): LargeObjectManagerIO[Unit] =
    PFLOM.delete(a)

  def open[A](a: Long, b: Int)(k: LargeObjectIO[A]): LargeObjectManagerIO[A] =
    PFLOM.open(a, b) >>= (PFLOM.liftLargeObject(_, k <* PFLO.close))

  def open[A](a: Long)(k: LargeObjectIO[A]): LargeObjectManagerIO[A] =
    PFLOM.open(a) >>= (PFLOM.liftLargeObject(_, k <* PFLO.close))

  def unlink(a: Long): LargeObjectManagerIO[Unit] =
    PFLOM.unlink(a)

#+scalaz
  def createLOFromFile(blockSize: Int, file: File): LargeObjectManagerIO[Long] =
    createLO >>= { oid => open(oid)(PHLO.copyFromFile(blockSize, file)).as(oid) }

  def createFileFromLO(blockSize: Int, oid: Long, file: File): LargeObjectManagerIO[Unit] =
    open(oid)(PHLO.copyToFile(blockSize, file))
#-scalaz

}
