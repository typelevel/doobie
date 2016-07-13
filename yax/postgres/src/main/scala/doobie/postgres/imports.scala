// package doobie.postgres

// /** 
//  * Module of aliases for commonly-used PostgreSQL types and syntax; use as 
//  * `import doobie.postgres.imports._` 
//  */
// object imports {

//   /**
//    * Alias for `doobie.postgres.free.copyin`
//    * @group PostgreSQL Free Module Aliases
//    */
//   val PFCI = doobie.postgres.free.copyin
  
//   /**
//    * Alias for `doobie.postgres.free.copymanager`
//    * @group PostgreSQL Free Module Aliases
//    */
//   val PFCM = doobie.postgres.free.copymanager
  
//   /**
//    * Alias for `doobie.postgres.free.copyout`
//    * @group PostgreSQL Free Module Aliases
//    */
//   val PFCO = doobie.postgres.free.copyout
  
//   /**
//    * Alias for `doobie.postgres.free.fastpath`
//    * @group PostgreSQL Free Module Aliases
//    */
//   val PFFP = doobie.postgres.free.fastpath
  
//   /**
//    * Alias for `doobie.postgres.free.largeobject`
//    * @group PostgreSQL Free Module Aliases
//    */
//   val PFLO = doobie.postgres.free.largeobject
  
//   /**
//    * Alias for `doobie.postgres.free.largeobjectmanager`
//    * @group PostgreSQL Free Module Aliases
//    */
//   val PFLOM = doobie.postgres.free.largeobjectmanager
  
//   /**
//    * Alias for `doobie.postgres.free.pgconnection`
//    * @group PostgreSQL Free Module Aliases
//    */
//   val PFPC = doobie.postgres.free.pgconnection

//   /**
//    * Alias for `doobie.postgres.hi.pgconnection`
//    * @group PostgreSQL Hi Module Aliases
//    */
//   val PHPC = doobie.postgres.hi.pgconnection

//   /**
//    * Alias for `doobie.postgres.hi.connection`
//    * @group PostgreSQL Hi Module Aliases
//    */
//   val PHC = doobie.postgres.hi.connection

//   /**
//    * Alias for `doobie.postgres.hi.largeobject`
//    * @group PostgreSQL Hi Module Aliases
//    */
//   val PHLO = doobie.postgres.hi.largeobject

//   /**
//    * Alias for `doobie.postgres.hi.largeobjectmanager`
//    * @group PostgreSQL Hi Module Aliases
//    */
//   val PHLOM = doobie.postgres.hi.largeobjectmanager

//   /** @group Type Aliass */ type CopyInIO[A]             = PFCI.CopyInIO[A]
//   /** @group Type Aliass */ type CopyManagerIO[A]        = PFCM.CopyManagerIO[A]
//   /** @group Type Aliass */ type CopyOutIO[A]            = PFCO.CopyOutIO[A]
//   /** @group Type Aliass */ type FastpathIO[A]           = PFFP.FastpathIO[A]
//   /** @group Type Aliass */ type LargeObjectIO[A]        = PFLO.LargeObjectIO[A]
//   /** @group Type Aliass */ type LargeObjectManagerIO[A] = PFLOM.LargeObjectManagerIO[A]
//   /** @group Type Aliass */ type PGConnectionIO[A]       = PFPC.PGConnectionIO[A]

// }