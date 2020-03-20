// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.free

import cats.effect.Async

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

  implicit lazy val AsyncCopyInIO: Async[copyin.CopyInIO] =
    copyin.AsyncCopyInIO

  implicit lazy val AsyncCopyManagerIO: Async[copymanager.CopyManagerIO] =
    copymanager.AsyncCopyManagerIO

  implicit lazy val AsyncCopyOutIO: Async[copyout.CopyOutIO] =
    copyout.AsyncCopyOutIO

  implicit lazy val AsyncFastpathIO: Async[fastpath.FastpathIO] =
    fastpath.AsyncFastpathIO

  implicit lazy val AsyncLargeObjectIO: Async[largeobject.LargeObjectIO] =
    largeobject.AsyncLargeObjectIO

  implicit lazy val AsyncLargeObjectManagerIO: Async[largeobjectmanager.LargeObjectManagerIO] =
    largeobjectmanager.AsyncLargeObjectManagerIO

  implicit lazy val AsyncPGConnectionIO: Async[pgconnection.PGConnectionIO] =
    pgconnection.AsyncPGConnectionIO
    
}
