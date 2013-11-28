package doobie
package std

import doobie._

object default extends default

trait default {

  implicit val bigdecimal   = doobie.std.bigdecimal.default

  implicit val int     = doobie.std.int.default
  implicit val string  = doobie.std.string.default
  implicit val url     = doobie.std.url.default

  ////// JAVA TYPES

  implicit val jbigdecimal = doobie.std.javastd.bigdecimal.default

}


