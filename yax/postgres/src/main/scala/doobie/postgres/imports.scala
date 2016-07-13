package doobie.contrib.postgresql

/** 
 * Module of aliases for commonly-used PostgreSQL types and syntax; use as 
 * `import doobie.contrib.postgresql.imports._` 
 */
object imports {

  /**
   * Alias for `doobie.contrib.postgresql.free.copyin`
   * @group PostgreSQL Free Module Aliases
   */
  val PFCI = doobie.contrib.postgresql.free.copyin
  
  /**
   * Alias for `doobie.contrib.postgresql.free.copymanager`
   * @group PostgreSQL Free Module Aliases
   */
  val PFCM = doobie.contrib.postgresql.free.copymanager
  
  /**
   * Alias for `doobie.contrib.postgresql.free.copyout`
   * @group PostgreSQL Free Module Aliases
   */
  val PFCO = doobie.contrib.postgresql.free.copyout
  
  /**
   * Alias for `doobie.contrib.postgresql.free.fastpath`
   * @group PostgreSQL Free Module Aliases
   */
  val PFFP = doobie.contrib.postgresql.free.fastpath
  
  /**
   * Alias for `doobie.contrib.postgresql.free.largeobject`
   * @group PostgreSQL Free Module Aliases
   */
  val PFLO = doobie.contrib.postgresql.free.largeobject
  
  /**
   * Alias for `doobie.contrib.postgresql.free.largeobjectmanager`
   * @group PostgreSQL Free Module Aliases
   */
  val PFLOM = doobie.contrib.postgresql.free.largeobjectmanager
  
  /**
   * Alias for `doobie.contrib.postgresql.free.pgconnection`
   * @group PostgreSQL Free Module Aliases
   */
  val PFPC = doobie.contrib.postgresql.free.pgconnection

  /**
   * Alias for `doobie.contrib.postgresql.hi.pgconnection`
   * @group PostgreSQL Hi Module Aliases
   */
  val PHPC = doobie.contrib.postgresql.hi.pgconnection

  /**
   * Alias for `doobie.contrib.postgresql.hi.connection`
   * @group PostgreSQL Hi Module Aliases
   */
  val PHC = doobie.contrib.postgresql.hi.connection

  /**
   * Alias for `doobie.contrib.postgresql.hi.largeobject`
   * @group PostgreSQL Hi Module Aliases
   */
  val PHLO = doobie.contrib.postgresql.hi.largeobject

  /**
   * Alias for `doobie.contrib.postgresql.hi.largeobjectmanager`
   * @group PostgreSQL Hi Module Aliases
   */
  val PHLOM = doobie.contrib.postgresql.hi.largeobjectmanager

  /** @group Type Aliass */ type CopyInIO[A]             = PFCI.CopyInIO[A]
  /** @group Type Aliass */ type CopyManagerIO[A]        = PFCM.CopyManagerIO[A]
  /** @group Type Aliass */ type CopyOutIO[A]            = PFCO.CopyOutIO[A]
  /** @group Type Aliass */ type FastpathIO[A]           = PFFP.FastpathIO[A]
  /** @group Type Aliass */ type LargeObjectIO[A]        = PFLO.LargeObjectIO[A]
  /** @group Type Aliass */ type LargeObjectManagerIO[A] = PFLOM.LargeObjectManagerIO[A]
  /** @group Type Aliass */ type PGConnectionIO[A]       = PFPC.PGConnectionIO[A]

}