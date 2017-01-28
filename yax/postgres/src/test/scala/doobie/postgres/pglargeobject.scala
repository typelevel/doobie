package doobie.postgres

import doobie.imports._
import doobie.postgres.imports._
import doobie.util.iolite.IOLite

import java.io.{File, FileInputStream, FileOutputStream}

import org.postgresql.PGNotification
import org.specs2.mutable.Specification

#+scalaz
import scalaz._, Scalaz._
#-scalaz
#+cats
import cats.implicits._
import fs2.interop.cats._
#-cats

object pglargeobjectspec extends Specification with FileEquality {

  val xa = DriverManagerTransactor[IOLite](
    "org.postgresql.Driver",
    "jdbc:postgresql:world",
    "postgres", ""
  )

  "large object support" should {

    "allow round-trip from file to large object and back" in  {
      val in   = new File("world.sql")
      val out  = File.createTempFile("doobie", "tst")
      val prog = PHLOM.createLOFromFile(1024 * 16, in) >>= { oid =>
        PHLOM.createFileFromLO(1024 * 16, oid, out) *> PHLOM.delete(oid)
      }
      PHC.pgGetLargeObjectAPI(prog).transact(xa).unsafePerformIO
      filesEqual(in, out)
      out.delete()
    }

    "allow round-trip from stream to large object and back" in  {
      val in   = new File("world.sql")
      val out  = File.createTempFile("doobie", "tst")
      val is = new FileInputStream(in)
      val os = new FileOutputStream(out)
      val prog = PHLOM.createLOFromStream(1024 * 16, is) >>= { oid =>
        PHLOM.createStreamFromLO(1024 * 16, oid, os) *> PHLOM.delete(oid)
      }
      PHC.pgGetLargeObjectAPI(prog).transact(xa).unsafePerformIO
      filesEqual(in, out)
      out.delete()
    }

  }

}

trait FileEquality {

  import java.io.{ Closeable, File, FileInputStream }
  import java.nio.ByteBuffer
  import java.nio.channels.FileChannel

  def using[A <: Closeable, B](a: A)(f: A => B): B =
    try f(a) finally a.close()

  def mapIn[A](file: File)(f: ByteBuffer => A): A =
    using(new FileInputStream(file)) { fis =>
      f(fis.getChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length))
    }

  def filesEqual(f1: File, f2: File): Boolean =
    mapIn(f1) { bb1 =>
    mapIn(f2) { bb2 =>
      bb1 == bb2
    }}

}
