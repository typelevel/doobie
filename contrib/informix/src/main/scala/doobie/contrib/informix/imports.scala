package doobie.contrib.informix

/** 
 * Module of aliases for commonly-used InformixSQL types and syntax; use as 
 * `import doobie.contrib.informix.imports._` 
 */
object imports {

  /**
   * Alias for `doobie.contrib.informix.free.ifxsmartblob`
   * @group InformixSQL Free Module Aliases
   */
  val IFXFSB = doobie.contrib.informix.free.ifxsmartblob
  
  /**
   * Alias for `doobie.contrib.informix.free.ifxconnection`
   * @group InformixSQL Free Module Aliases
   */
  val IFXFLC = doobie.contrib.informix.free.ifxconnection

  /**
   * Alias for `doobie.contrib.informix.hi.ifxconnection`
   * @group InformixSQL Hi Module Aliases
   */
  val IFXFHC = doobie.contrib.informix.hi.ifxconnection

  /**
   * Alias for `doobie.contrib.informix.hi.connection`
   * @group InformixSQL Hi Module Aliases
   */
  val IFXHC = doobie.contrib.informix.hi.connection

  /** @group Type Aliass */ type IfxSmartBlobIO[A]        = IFXFSB.IfxSmartBlobIO[A]
  /** @group Type Aliass */ type IfxConnectionIO[A]       = IFXFLC.IfxConnectionIO[A]

}
