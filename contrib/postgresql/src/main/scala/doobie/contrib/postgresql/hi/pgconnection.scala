package doobie.contrib.postgresql.hi

import org.postgresql.{ PGConnection, PGNotification }
import org.postgresql.fastpath.Fastpath

import doobie.contrib.postgresql.free.{ pgconnection => PGC }
import doobie.contrib.postgresql.free.pgconnection.PGConnectionIO
import doobie.contrib.postgresql.free.copymanager.CopyManagerIO
import doobie.contrib.postgresql.free.fastpath.FastpathIO
import doobie.contrib.postgresql.free.largeobjectmanager.LargeObjectManagerIO

import doobie.imports._

object pgconnection {

  val getBackendPID: PGConnectionIO[Int] =
    PGC.getBackendPID

  def getCopyAPI[A](k: CopyManagerIO[A]): PGConnectionIO[A] =
    PGC.getCopyAPI.flatMap(s => PGC.liftCopyManager(s, k)) // N.B. no need to close()

  def getFastpathAPI[A](k: FastpathIO[A]): PGConnectionIO[A] =
    PGC.getFastpathAPI.flatMap(s => PGC.liftFastpath(s, k)) // N.B. no need to close()

  def getLargeObjectAPI[A](k: LargeObjectManagerIO[A]): PGConnectionIO[A] =
    PGC.getLargeObjectAPI.flatMap(s => PGC.liftLargeObjectManager(s, k)) // N.B. no need to close()

  val getNotifications: PGConnectionIO[List[PGNotification]] =
    PGC.getNotifications map {
      case null => Nil
      case ns   => ns.toList
    }

  val getPrepareThreshold: PGConnectionIO[Int] =
    PGC.getPrepareThreshold

  def setPrepareThreshold(threshold: Int): PGConnectionIO[Unit] =
    PGC.setPrepareThreshold(threshold)

}
