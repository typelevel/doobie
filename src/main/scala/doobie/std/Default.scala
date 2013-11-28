package doobie
package std

import doobie.JdbcType

/** 
 * Default JDBC bindings. If you don't like these, override `default` or replace it
 * entirely. Sensible mappings are available a-la-carte for each primitive type, but you
 * are free to construct unreasonable mappings if you wish. There's nothing complicated
 * or secret happening here. Go nuts.
 */
object default extends default

trait default {

  implicit val dint     = int.integer
  implicit val dstring  = string.varchar

  // // TODO: what is the most reasonable default, or is there one?
  // implicit val dboolean = boolean.asZeroOrOne[JdbcType.INTEGER]

}

