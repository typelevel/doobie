package doobie.postgres.free

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
  implicit lazy val AsyncCopyInIO             = copyin.AsyncCopyInIO
  implicit lazy val AsyncCopyManagerIO        = copymanager.AsyncCopyManagerIO
  implicit lazy val AsyncCopyOutIO            = copyout.AsyncCopyOutIO
  implicit lazy val AsyncFastpathIO           = fastpath.AsyncFastpathIO
  implicit lazy val AsyncLargeObjectIO        = largeobject.AsyncLargeObjectIO
  implicit lazy val AsyncLargeObjectManagerIO = largeobjectmanager.AsyncLargeObjectManagerIO
  implicit lazy val AsyncPGConnectionIO       = pgconnection.AsyncPGConnectionIO
}
