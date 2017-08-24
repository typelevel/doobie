package doobie.postgres.hi

import org.postgresql.PGNotification

object pgconnection {

  val getBackendPID: PGConnectionIO[Int] =
    PFPC.getBackendPID

  def getCopyAPI[A](k: CopyManagerIO[A]): PGConnectionIO[A] =
    PFPC.getCopyAPI.flatMap(s => PFPC.embed(s, k)) // N.B. no need to close()

  def getFastpathAPI[A](k: FastpathIO[A]): PGConnectionIO[A] =
    PFPC.getFastpathAPI.flatMap(s => PFPC.embed(s, k)) // N.B. no need to close()

  def getLargeObjectAPI[A](k: LargeObjectManagerIO[A]): PGConnectionIO[A] =
    PFPC.getLargeObjectAPI.flatMap(s => PFPC.embed(s, k)) // N.B. no need to close()

  val getNotifications: PGConnectionIO[List[PGNotification]] =
    PFPC.getNotifications map {
      case null => Nil
      case ns   => ns.toList
    }

  val getPrepareThreshold: PGConnectionIO[Int] =
    PFPC.getPrepareThreshold

  def setPrepareThreshold(threshold: Int): PGConnectionIO[Unit] =
    PFPC.setPrepareThreshold(threshold)

}
