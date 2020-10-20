// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.free

import cats.effect.{ MonadCancel, Sync }

trait Types {
  type CopyInIO[A]             = copyin.CopyInIO[A]
  type CopyManagerIO[A]        = copymanager.CopyManagerIO[A]
  type CopyOutIO[A]            = copyout.CopyOutIO[A]
  type FastpathIO[A]           = fastpath.FastpathIO[A]
  type LargeObjectIO[A]        = largeobject.LargeObjectIO[A]
  type LargeObjectManagerIO[A] = largeobjectmanager.LargeObjectManagerIO[A]
  type PGConnectionIO[A]       = pgconnection.PGConnectionIO[A]
}

trait Modules {
  lazy val PFCI  = copyin
  lazy val PFCM  = copymanager
  lazy val PFCO  = copyout
  lazy val PFFP  = fastpath
  lazy val PFLO  = largeobject
  lazy val PFLOM = largeobjectmanager
  lazy val PFPC  = pgconnection
}

trait Instances {

  implicit lazy val SyncMonadCancelCopyInIO: Sync[copyin.CopyInIO] with MonadCancel[copyin.CopyInIO, Throwable] =
    copyin.SyncMonadCancelCopyInIO

  implicit lazy val SyncMonadCancelCopyManagerIO: Sync[copymanager.CopyManagerIO] with MonadCancel[copymanager.CopyManagerIO, Throwable] =
    copymanager.SyncMonadCancelCopyManagerIO

  implicit lazy val SyncMonadCancelCopyOutIO: Sync[copyout.CopyOutIO] with MonadCancel[copyout.CopyOutIO, Throwable] =
    copyout.SyncMonadCancelCopyOutIO

  implicit lazy val SyncMonadCancelFastpathIO: Sync[fastpath.FastpathIO] with MonadCancel[fastpath.FastpathIO, Throwable] =
    fastpath.SyncMonadCancelFastpathIO

  implicit lazy val SyncMonadCancelLargeObjectIO: Sync[largeobject.LargeObjectIO] with MonadCancel[largeobject.LargeObjectIO, Throwable] =
    largeobject.SyncMonadCancelLargeObjectIO

  implicit lazy val SyncMonadCancelLargeObjectManagerIO: Sync[largeobjectmanager.LargeObjectManagerIO] with MonadCancel[largeobjectmanager.LargeObjectManagerIO, Throwable] =
    largeobjectmanager.SyncMonadCancelLargeObjectManagerIO

  implicit lazy val SyncMonadCancelPGConnectionIO: Sync[pgconnection.PGConnectionIO] with MonadCancel[pgconnection.PGConnectionIO, Throwable] =
    pgconnection.SyncMonadCancelPGConnectionIO

}
