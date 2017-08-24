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
  val PFCI  = copyin
  val PFCM  = copymanager
  val PFCO  = copyout
  val PFFP  = fastpath
  val PFLO  = largeobject
  val PFLOM = largeobjectmanager
  val PFPC  = pgconnection
}

trait Instances {
  implicit val AsyncCopyInIO             = copyin.AsyncCopyInIO
  implicit val AsyncCopyManagerIO        = copymanager.AsyncCopyManagerIO
  implicit val AsyncCopyOutIO            = copyout.AsyncCopyOutIO
  implicit val AsyncFastpathIO           = fastpath.AsyncFastpathIO
  implicit val AsyncLargeObjectIO        = largeobject.AsyncLargeObjectIO
  implicit val AsyncLargeObjectManagerIO = largeobjectmanager.AsyncLargeObjectManagerIO
  implicit val AsyncPGConnectionIO       = pgconnection.AsyncPGConnectionIO
}
