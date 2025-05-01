// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.hi

import org.postgresql.PGNotification
import doobie.postgres.free.pgconnection as IPFPC

object pgconnection {

  val getBackendPID: PGConnectionIO[Int] =
    IPFPC.getBackendPID

  def getCopyAPI[A](k: CopyManagerIO[A]): PGConnectionIO[A] =
    IPFPC.getCopyAPI.flatMap(s => IPFPC.embed(s, k)) // N.B. no need to close()

  def getLargeObjectAPI[A](k: LargeObjectManagerIO[A]): PGConnectionIO[A] =
    IPFPC.getLargeObjectAPI.flatMap(s => IPFPC.embed(s, k)) // N.B. no need to close()

  val getNotifications: PGConnectionIO[List[PGNotification]] =
    IPFPC.getNotifications map {
      case null => Nil
      case ns   => ns.toList
    }

  val getPrepareThreshold: PGConnectionIO[Int] =
    IPFPC.getPrepareThreshold

  def setPrepareThreshold(threshold: Int): PGConnectionIO[Unit] =
    IPFPC.setPrepareThreshold(threshold)

}
